package org.rotary.goodsassistant

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import org.rotary.goodsassistant.data.GasRepository
import org.rotary.goodsassistant.data.PreferencesManager
import org.rotary.goodsassistant.databinding.ActivityMainBinding
import org.rotary.goodsassistant.model.GoodsItem
import org.rotary.goodsassistant.ui.QueueAdapter
import org.rotary.goodsassistant.util.GoodsInjector

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private val gasRepo = GasRepository()
    private lateinit var queueAdapter: QueueAdapter

    private var currentStagedItem: GoodsItem? = null
    private val goodsAddUrl = "https://www.17rcn.org/member/goods_add.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        setupViews()
        setupWebView()
        loadInitialData()
    }

    private fun setupViews() {
        // 1. RecyclerView 待刊佇列
        queueAdapter = QueueAdapter { item ->
            startPublishItem(item)
        }
        binding.rvQueue.layoutManager = LinearLayoutManager(this)
        binding.rvQueue.adapter = queueAdapter

        // 2. 標籤列切換監聽
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> switchTab(0)
                    1 -> switchTab(1)
                    2 -> switchTab(2)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 3. 頂部按鈕監聽
        binding.btnHeaderRefresh.setOnClickListener {
            refreshQueue()
        }

        binding.btnBackQueue.setOnClickListener {
            binding.tabLayout.getTabAt(0)?.select()
        }

        binding.btnReinject.setOnClickListener {
            injectCurrentStagedItem()
        }

        // 4. 設定頁面儲存
        binding.btnSaveSettings.setOnClickListener {
            val gasUrl = binding.etGasUrl.text.toString().trim()
            val addr = binding.etDefaultAddress.text.toString().trim()
            prefs.gasUrl = gasUrl
            prefs.defaultAddress = addr
            Toast.makeText(this, "✅ 設定已儲存", Toast.LENGTH_SHORT).show()
            binding.tabLayout.getTabAt(0)?.select()
            refreshQueue()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webView
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.databaseEnabled = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // 保持 Session Cookie
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && url.contains("goods_add.php")) {
                    if (currentStagedItem != null) {
                        injectCurrentStagedItem()
                    }
                }
            }
        }

        // 注入原生通訊 Bridge
        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")

        // 初始載入刊登網址
        webView.loadUrl(goodsAddUrl)
    }

    private fun loadInitialData() {
        binding.etGasUrl.setText(prefs.gasUrl)
        binding.etDefaultAddress.setText(prefs.defaultAddress)

        if (prefs.gasUrl.isNotBlank()) {
            refreshQueue()
        } else {
            binding.tvQueueStatus.text = "請先至「偏好設定」分頁填入 Google Apps Script 網址"
            binding.tvQueueStatus.visibility = View.VISIBLE
        }
    }

    private fun refreshQueue() {
        val gasUrl = prefs.gasUrl
        if (gasUrl.isBlank()) {
            binding.tvQueueStatus.text = "⚠️ 請先至「偏好設定」輸入 Google Apps Script 部署網址"
            binding.tvQueueStatus.visibility = View.VISIBLE
            return
        }

        binding.queueProgressBar.visibility = View.VISIBLE
        binding.tvQueueStatus.visibility = View.GONE

        lifecycleScope.launch {
            val result = gasRepo.getQueue(gasUrl)
            binding.queueProgressBar.visibility = View.GONE
            result.onSuccess { items ->
                if (items.isEmpty()) {
                    binding.tvQueueStatus.text = getString(R.string.status_empty)
                    binding.tvQueueStatus.visibility = View.VISIBLE
                    queueAdapter.submitList(emptyList())
                } else {
                    binding.tvQueueStatus.visibility = View.GONE
                    queueAdapter.submitList(items)
                }
            }.onFailure { err ->
                binding.tvQueueStatus.text = "❌ 連線錯誤：${err.message}"
                binding.tvQueueStatus.visibility = View.VISIBLE
            }
        }
    }

    private fun startPublishItem(item: GoodsItem) {
        currentStagedItem = item
        binding.tvStagedItemName.text = "準備刊登：${item.title ?: "物資"}"
        Toast.makeText(this, "正在預載圖片並準備刊登...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            // 背景預先下載照片 base64
            val gasUrl = prefs.gasUrl
            if (item.photos.isNotEmpty() && gasUrl.isNotBlank()) {
                val preparedPhotos = gasRepo.preparePhotosBase64(item.photos, gasUrl)
                currentStagedItem = item.copy(photos = preparedPhotos)
            }

            // 鎖定該列
            if (item.row > 0 && gasUrl.isNotBlank()) {
                gasRepo.lockItem(gasUrl, item.row)
            }

            // 切換至 WebView 分頁並載入刊登頁面
            binding.tabLayout.getTabAt(1)?.select()
            val currentUrl = binding.webView.url
            if (currentUrl != null && currentUrl.contains("goods_add.php")) {
                injectCurrentStagedItem()
            } else {
                binding.webView.loadUrl(goodsAddUrl)
            }
        }
    }

    private fun injectCurrentStagedItem() {
        val item = currentStagedItem ?: return
        val script = GoodsInjector.buildInjectionScript(item, prefs.defaultAddress)
        binding.webView.evaluateJavascript(script) { result ->
            Toast.makeText(this, "⚡ 物資資料已自動填入！請確認後輸入驗證碼送出", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchTab(tabIndex: Int) {
        binding.layoutQueue.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.layoutBrowser.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.layoutSettings.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onFormSubmitted() {
            runOnUiThread {
                val completedItem = currentStagedItem
                if (completedItem != null && completedItem.row > 0) {
                    val gasUrl = prefs.gasUrl
                    if (gasUrl.isNotBlank()) {
                        lifecycleScope.launch {
                            gasRepo.markCompleted(gasUrl, completedItem.row)
                        }
                    }
                }

                AlertDialog.Builder(this@MainActivity)
                    .setTitle("🎉 刊登送出成功")
                    .setMessage("物資【${completedItem?.title ?: ""}】已送出，系統已自動向 Google 試算表標記「已刊登」！")
                    .setPositiveButton("返回物資佇列") { _, _ ->
                        currentStagedItem = null
                        binding.tvStagedItemName.text = "準備刊登：無"
                        binding.tabLayout.getTabAt(0)?.select()
                        refreshQueue()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.layoutBrowser.visibility == View.VISIBLE && binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else if (binding.layoutBrowser.visibility == View.VISIBLE || binding.layoutSettings.visibility == View.VISIBLE) {
            binding.tabLayout.getTabAt(0)?.select()
        } else {
            super.onBackPressed()
        }
    }
}

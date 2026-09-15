package org.rotary.goodsassistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import org.rotary.goodsassistant.data.GasRepository
import org.rotary.goodsassistant.data.PreferencesManager
import org.rotary.goodsassistant.databinding.ActivityMainBinding
import org.rotary.goodsassistant.model.GoodsItem
import org.rotary.goodsassistant.ui.QueueAdapter
import org.rotary.goodsassistant.util.CompressedPhoto
import org.rotary.goodsassistant.util.GoodsInjector
import org.rotary.goodsassistant.util.ImageCompressor
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private val gasRepo = GasRepository()
    private lateinit var queueAdapter: QueueAdapter

    private var currentStagedItem: GoodsItem? = null
    private val goodsAddUrl = "https://www.17rcn.org/member/goods_add.php"

    // 拍照採集狀態管理 (最多 3 張)
    private val capturedPhotos = mutableListOf<CompressedPhoto>()
    private var tempCameraUri: Uri? = null

    // 相機拍照 ActivityResultLauncher
    private val takePictureLauncher: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempCameraUri?.let { uri ->
                    processAndAddImage(uri)
                }
            }
        }

    // 相簿多選 ActivityResultLauncher
    private val pickMultipleMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris ->
            if (uris.isNotEmpty()) {
                val remainingSlots = 3 - capturedPhotos.size
                val urisToProcess = uris.take(remainingSlots)
                for (uri in urisToProcess) {
                    processAndAddImage(uri)
                }
            }
        }

    // 相機權限請求 Launcher
    private val requestCameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                launchCameraCapture()
            } else {
                Toast.makeText(this, "需授予相機權限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        setupViews()
        setupCaptureTab()
        setupWebView()
        loadInitialData()
    }

    private fun setupViews() {
        // 1. RecyclerView 待刊佇列
        queueAdapter = QueueAdapter(
            onPublishClick = { item -> startPublishItem(item) },
            onReprocessClick = { item -> reprocessItem(item) },
            onDeleteClick = { item -> confirmDeleteItem(item) }
        )
        binding.rvQueue.layoutManager = LinearLayoutManager(this)
        binding.rvQueue.adapter = queueAdapter

        // 2. 標籤列切換監聽 (4分頁: 0=採集, 1=佇列, 2=瀏覽器, 3=設定)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { switchTab(it) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 3. 頂部按鈕監聽
        binding.btnHeaderRefresh.setOnClickListener {
            refreshQueue()
        }

        binding.btnBackQueue.setOnClickListener {
            binding.tabLayout.getTabAt(1)?.select()
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
            binding.etCaptureAddress.setText(addr)
            Toast.makeText(this, "✅ 設定已儲存", Toast.LENGTH_SHORT).show()
            binding.tabLayout.getTabAt(1)?.select()
            refreshQueue()
        }

        switchTab(0)
    }

    private fun setupCaptureTab() {
        // 點擊拍照按鈕
        binding.btnCaptureCamera.setOnClickListener {
            if (capturedPhotos.size >= 3) {
                Toast.makeText(this, "最多只能上傳 3 張照片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCameraCapture()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        // 點擊從相簿選取
        binding.btnCaptureGallery.setOnClickListener {
            if (capturedPhotos.size >= 3) {
                Toast.makeText(this, "最多只能上傳 3 張照片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickMultipleMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 刪除照片按鈕
        binding.btnRemovePhoto1.setOnClickListener { removePhotoAt(0) }
        binding.btnRemovePhoto2.setOnClickListener { removePhotoAt(1) }
        binding.btnRemovePhoto3.setOnClickListener { removePhotoAt(2) }

        // 送出採集與 AI 辨識
        binding.btnSubmitCapture.setOnClickListener {
            submitCapturedItem()
        }

        updatePhotoSlotsUI()
    }

    private fun launchCameraCapture() {
        try {
            val photoFile = File.createTempFile("captured_photo_", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "無法建立暫存照片檔案：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processAndAddImage(uri: Uri) {
        if (capturedPhotos.size >= 3) return

        binding.captureProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val compressed = ImageCompressor.compressUri(this@MainActivity, uri, maxDimension = 1600, quality = 80)
            binding.captureProgressBar.visibility = View.GONE

            if (compressed != null) {
                if (capturedPhotos.size < 3) {
                    capturedPhotos.add(compressed)
                    updatePhotoSlotsUI()
                }
            } else {
                Toast.makeText(this@MainActivity, "圖片處理失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removePhotoAt(index: Int) {
        if (index in 0 until capturedPhotos.size) {
            capturedPhotos.removeAt(index)
            updatePhotoSlotsUI()
        }
    }

    private fun updatePhotoSlotsUI() {
        val imageViews = listOf(binding.ivPhoto1, binding.ivPhoto2, binding.ivPhoto3)
        val textViews = listOf(binding.tvEmpty1, binding.tvEmpty2, binding.tvEmpty3)
        val removeBtns = listOf(binding.btnRemovePhoto1, binding.btnRemovePhoto2, binding.btnRemovePhoto3)

        for (i in 0 until 3) {
            if (i < capturedPhotos.size) {
                imageViews[i].setImageBitmap(capturedPhotos[i].bitmap)
                imageViews[i].visibility = View.VISIBLE
                textViews[i].visibility = View.GONE
                removeBtns[i].visibility = View.VISIBLE
            } else {
                imageViews[i].setImageDrawable(null)
                imageViews[i].visibility = View.GONE
                textViews[i].visibility = View.VISIBLE
                removeBtns[i].visibility = View.GONE
            }
        }
    }

    private fun submitCapturedItem() {
        val gasUrl = prefs.gasUrl
        if (gasUrl.isBlank()) {
            Toast.makeText(this, "⚠️ 請先至「偏好設定」輸入 Google Apps Script 網址", Toast.LENGTH_LONG).show()
            binding.tabLayout.getTabAt(3)?.select()
            return
        }

        if (capturedPhotos.isEmpty()) {
            Toast.makeText(this, "請至少拍攝或選擇 1 張物資照片", Toast.LENGTH_SHORT).show()
            return
        }

        val address = binding.etCaptureAddress.text.toString().trim()
        val note = binding.etCaptureNote.text.toString().trim()
        val base64Photos = capturedPhotos.map { it.base64 }

        binding.captureProgressBar.visibility = View.VISIBLE
        binding.btnSubmitCapture.isEnabled = false

        lifecycleScope.launch {
            val result = gasRepo.uploadItem(gasUrl, base64Photos, address, note)
            binding.captureProgressBar.visibility = View.GONE
            binding.btnSubmitCapture.isEnabled = true

            result.onSuccess {
                Toast.makeText(this@MainActivity, "🎉 上傳成功！物資已進入排程並開始 AI 辨識", Toast.LENGTH_LONG).show()
                // 重置採集表單
                capturedPhotos.clear()
                updatePhotoSlotsUI()
                binding.etCaptureNote.setText("")
                // 切換至物資佇列分頁並自動刷新
                binding.tabLayout.getTabAt(1)?.select()
                refreshQueue()
            }.onFailure { err ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("❌ 上傳失敗")
                    .setMessage(err.message ?: "無法連接 Google 試算表伺服器")
                    .setPositiveButton("確定", null)
                    .show()
            }
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
        
        // 預設地址填入偏好設定與採集輸入框
        val defaultAddr = prefs.defaultAddress
        if (defaultAddr.isNotBlank()) {
            binding.etDefaultAddress.setText(defaultAddr)
            binding.etCaptureAddress.setText(defaultAddr)
        }

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
        binding.tvStagedItemName.text = "📦 準備刊登：${item.title ?: "物資"}"
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

            // 切換至 WebView 分頁 (Tab 2) 並載入刊登頁面
            binding.tabLayout.getTabAt(2)?.select()
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
        val defaultAddr = binding.etDefaultAddress.text.toString().trim().ifBlank { prefs.defaultAddress }
        val script = GoodsInjector.buildInjectionScript(item, defaultAddr)
        binding.webView.evaluateJavascript(script) {
            Toast.makeText(this, "⚡ 物資資料已自動填入！請確認後輸入驗證碼送出", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reprocessItem(item: GoodsItem) {
        val gasUrl = prefs.gasUrl
        if (gasUrl.isBlank()) {
            Toast.makeText(this, "⚠️ 請先至偏好設定輸入 GAS 部署網址", Toast.LENGTH_SHORT).show()
            return
        }

        binding.queueProgressBar.visibility = View.VISIBLE
        Toast.makeText(this, "⏳ 正在重新 AI 辨識第 ${item.row} 列物資...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val result = gasRepo.reprocessRow(gasUrl, item.row)
            binding.queueProgressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this@MainActivity, "🎉 第 ${item.row} 列物資已重新辨識完成！", Toast.LENGTH_SHORT).show()
                refreshQueue()
            }.onFailure { err ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("❌ 重新辨識失敗")
                    .setMessage(err.message ?: "發生未知的錯誤")
                    .setPositiveButton("確定", null)
                    .show()
            }
        }
    }

    private fun confirmDeleteItem(item: GoodsItem) {
        val gasUrl = prefs.gasUrl
        if (gasUrl.isBlank()) {
            Toast.makeText(this, "⚠️ 請先至偏好設定輸入 GAS 部署網址", Toast.LENGTH_SHORT).show()
            return
        }

        if (item.status == "刊登中") {
            Toast.makeText(this, "⚠️ 此物資目前正在刊登中，無法刪除！", Toast.LENGTH_LONG).show()
            return
        }

        val itemName = item.title?.ifBlank { null } ?: "無品名"
        val message = getString(R.string.dialog_delete_message, itemName, item.row)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_delete_confirm) { _, _ ->
                executeDeleteItem(item)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun executeDeleteItem(item: GoodsItem) {
        val gasUrl = prefs.gasUrl
        binding.queueProgressBar.visibility = View.VISIBLE
        Toast.makeText(this, "⏳ 正在刪除第 ${item.row} 列物資與雲端照片...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val result = gasRepo.deleteItem(gasUrl, item.row)
            binding.queueProgressBar.visibility = View.GONE
            result.onSuccess {
                Toast.makeText(this@MainActivity, "✅ 第 ${item.row} 列物資已成功刪除！", Toast.LENGTH_SHORT).show()
                refreshQueue()
            }.onFailure { err ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("❌ 刪除失敗")
                    .setMessage(err.message ?: "無法完成刪除")
                    .setPositiveButton("確定", null)
                    .show()
            }
        }
    }

    private fun switchTab(tabIndex: Int) {
        // 0: 拍照採集, 1: 物資佇列, 2: 刊登網頁, 3: 偏好設定
        binding.layoutCapture.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.layoutQueue.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.layoutBrowser.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        binding.layoutSettings.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE
        binding.btnHeaderRefresh.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
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
                        binding.tvStagedItemName.text = "📦 準備刊登：無"
                        binding.tabLayout.getTabAt(1)?.select()
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
        } else if (binding.layoutBrowser.visibility == View.VISIBLE || binding.layoutSettings.visibility == View.VISIBLE || binding.layoutCapture.visibility == View.VISIBLE) {
            binding.tabLayout.getTabAt(1)?.select()
        } else {
            super.onBackPressed()
        }
    }
}

package org.rotary.goodsassistant.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.rotary.goodsassistant.data.ImageCacheManager
import org.rotary.goodsassistant.model.PhotoItem
import java.util.concurrent.TimeUnit

object ImageLoader {

    private const val TAG = "ImageLoader"

    // 記憶體快取 (最多保留 60 張 Bitmap，避免頻繁解碼與閃爍)
    private val bitmapCache = LruCache<String, Bitmap>(60)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * 取得記憶體中已快取的縮圖
     */
    fun getCachedBitmap(cacheKey: String): Bitmap? {
        if (cacheKey.isBlank()) return null
        return bitmapCache.get(cacheKey)
    }

    /**
     * 非同步載入縮圖到 ImageView，具備 Tag 防錯位檢查與快取
     */
    fun loadThumbnail(
        imageView: ImageView,
        photo: PhotoItem,
        scope: CoroutineScope,
        imageCacheManager: ImageCacheManager? = null,
        gasUrl: String = ""
    ) {
        val cacheKey = photo.fileId ?: photo.id ?: photo.thumbnailUrl ?: photo.originalUrl ?: ""
        if (cacheKey.isBlank()) {
            imageView.setImageDrawable(null)
            return
        }

        // 1. 命中記憶體快取時立即顯示
        val cached = bitmapCache.get(cacheKey)
        if (cached != null) {
            imageView.tag = cacheKey
            imageView.setImageBitmap(cached)
            return
        }

        // 2. 設定 Tag 並清除舊圖（避免列表快速滑動時錯位顯示舊圖）
        imageView.tag = cacheKey
        imageView.setImageDrawable(null)

        scope.launch {
            val bitmap = fetchBitmap(photo, cacheKey, imageCacheManager, gasUrl, preferOriginal = false)
            if (bitmap != null) {
                bitmapCache.put(cacheKey, bitmap)
                if (imageView.tag == cacheKey) {
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    /**
     * 非同步載入高解析大圖（供 Lightbox 燈箱全螢幕檢視使用）
     */
    suspend fun loadLargeBitmap(
        photo: PhotoItem,
        imageCacheManager: ImageCacheManager? = null,
        gasUrl: String = ""
    ): Bitmap? = withContext(Dispatchers.IO) {
        val largeKey = (photo.fileId ?: photo.id ?: photo.originalUrl ?: "") + "_large"
        val cached = bitmapCache.get(largeKey)
        if (cached != null) return@withContext cached

        // 優先嘗試 originalUrl，失敗則降級為 thumbnailUrl
        val bitmap = fetchBitmap(photo, largeKey, imageCacheManager, gasUrl, preferOriginal = true)
        if (bitmap != null) {
            bitmapCache.put(largeKey, bitmap)
        }
        bitmap
    }

    private suspend fun fetchBitmap(
        photo: PhotoItem,
        cacheKey: String,
        imageCacheManager: ImageCacheManager?,
        gasUrl: String,
        preferOriginal: Boolean
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // A. 若模型物件已帶有 Base64 資料
            if (!photo.base64Data.isNullOrBlank()) {
                val bytes = Base64.decode(photo.base64Data, Base64.DEFAULT)
                val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bm != null) return@withContext bm
            }

            // B. 檢查 L2 磁碟快取
            if (imageCacheManager != null && cacheKey.isNotBlank()) {
                val diskCached = imageCacheManager.get(cacheKey)
                if (diskCached != null && diskCached.base64Data.isNotBlank()) {
                    val bytes = Base64.decode(diskCached.base64Data, Base64.DEFAULT)
                    val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bm != null) return@withContext bm
                }
            }

            // C. 優先使用 Google Drive 直連或縮圖網址下載
            val primaryUrl = if (preferOriginal) {
                photo.originalUrl ?: photo.downloadUrl ?: photo.thumbnailUrl
            } else {
                photo.thumbnailUrl ?: photo.downloadUrl ?: photo.originalUrl
            }

            if (!primaryUrl.isNullOrBlank()) {
                try {
                    val req = Request.Builder().url(primaryUrl).get().build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bm != null) {
                                    if (imageCacheManager != null && cacheKey.isNotBlank()) {
                                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                        imageCacheManager.put(cacheKey, base64)
                                    }
                                    return@withContext bm
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Direct download failed for: $primaryUrl", e)
                }
            }

            // D. 降級備用：透過 GAS getImageBase64 後端代理轉出 Base64
            val fileId = photo.fileId ?: photo.id
            if (!fileId.isNullOrBlank() && gasUrl.isNotBlank()) {
                try {
                    val proxyUrl = if (gasUrl.contains("?")) {
                        "$gasUrl&action=getImageBase64&fileId=$fileId"
                    } else {
                        "$gasUrl?action=getImageBase64&fileId=$fileId"
                    }
                    val req = Request.Builder().url(proxyUrl).get().build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: ""
                            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
                            if (json.has("success") && json.get("success").asBoolean && json.has("base64")) {
                                val b64 = json.get("base64").asString
                                if (!b64.isNullOrBlank()) {
                                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                                    val bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bm != null) {
                                        if (imageCacheManager != null && cacheKey.isNotBlank()) {
                                            imageCacheManager.put(cacheKey, b64)
                                        }
                                        return@withContext bm
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "GAS proxy download failed for fileId: $fileId", e)
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "fetchBitmap failed: $cacheKey", e)
            null
        }
    }
}

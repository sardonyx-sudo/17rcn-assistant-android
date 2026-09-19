package org.rotary.goodsassistant.data

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.rotary.goodsassistant.model.GasImageResponse
import org.rotary.goodsassistant.model.GasResponse
import org.rotary.goodsassistant.model.GoodsItem
import org.rotary.goodsassistant.model.PhotoItem
import java.util.concurrent.TimeUnit

class GasRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    private fun appendParam(url: String, key: String, value: String): String {
        val cleanUrl = url.trim()
        val separator = if (cleanUrl.contains("?")) "&" else "?"
        return "$cleanUrl$separator$key=$value"
    }

    suspend fun getQueue(gasUrl: String): Result<List<GoodsItem>> = withContext(Dispatchers.IO) {
        try {
            if (gasUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("尚未設定 Google Apps Script 網址"))
            }

            val targetUrl = appendParam(gasUrl, "action", "getItems")
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GAS 伺服器回應錯誤 HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)
                val items = gasResponse.items ?: gasResponse.data?.items
                if (gasResponse.success && items != null) {
                    // 篩選待刊登或刊登中的項目 (支援前後空白容錯)
                    val pending = items.filter { 
                        val s = it.status?.trim() ?: ""
                        s.contains("待刊登") || s.contains("刊登中") 
                    }
                    Result.success(pending)
                } else {
                    val errMsg = gasResponse.error ?: gasResponse.message ?: "無法獲取佇列資料"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("GasRepository", "getQueue error", e)
            Result.failure(e)
        }
    }

    suspend fun lockItem(gasUrl: String, row: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var targetUrl = appendParam(gasUrl, "action", "lockItem")
            targetUrl = "$targetUrl&row=$row"
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GAS 伺服器錯誤 HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)
                if (gasResponse != null && gasResponse.success) {
                    Result.success(Unit)
                } else {
                    val errMsg = gasResponse?.error ?: gasResponse?.message ?: "此物資已被鎖定或狀態已變更"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.w("GasRepository", "lockItem failed", e)
            Result.failure(e)
        }
    }

    suspend fun unlockItem(gasUrl: String, row: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var targetUrl = appendParam(gasUrl, "action", "unlockItem")
            targetUrl = "$targetUrl&row=$row"
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GAS 伺服器錯誤 HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)
                if (gasResponse != null && gasResponse.success) {
                    Result.success(Unit)
                } else {
                    val errMsg = gasResponse?.error ?: gasResponse?.message ?: "解除鎖定失敗"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.w("GasRepository", "unlockItem failed", e)
            Result.failure(e)
        }
    }

    suspend fun markCompleted(gasUrl: String, row: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var targetUrl = appendParam(gasUrl, "action", "markPublished")
            targetUrl = "$targetUrl&row=$row"
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GAS 伺服器錯誤 HTTP ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)
                if (gasResponse != null && gasResponse.success) {
                    Result.success(Unit)
                } else {
                    val errMsg = gasResponse?.error ?: gasResponse?.message ?: "標記刊登完成失敗"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.w("GasRepository", "markCompleted failed", e)
            Result.failure(e)
        }
    }

    suspend fun reprocessRow(gasUrl: String, row: Int, model: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var targetUrl = appendParam(gasUrl, "action", "reprocessRow")
            targetUrl = "$targetUrl&row=$row"
            if (model.isNotBlank()) {
                targetUrl = "$targetUrl&model=$model"
            }
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP 錯誤 ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)
                if (gasResponse.success) {
                    Result.success(Unit)
                } else {
                    val errMsg = gasResponse.error ?: gasResponse.message ?: "重新辨識失敗"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("GasRepository", "reprocessRow failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteItem(gasUrl: String, row: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var targetUrl = appendParam(gasUrl, "action", "deleteItem")
            targetUrl = "$targetUrl&row=$row"
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP 錯誤 ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)
                if (gasResponse.success) {
                    Result.success(Unit)
                } else {
                    val errMsg = gasResponse.error ?: gasResponse.message ?: "刪除物資失敗"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("GasRepository", "deleteItem failed", e)
            Result.failure(e)
        }
    }

    suspend fun preparePhotosBase64(
        photos: List<PhotoItem>,
        gasUrl: String,
        imageCacheManager: ImageCacheManager? = null
    ): List<PhotoItem> = withContext(Dispatchers.IO) {
        photos.take(3).map { photo ->
            var loaded = false
            val fileId = photo.fileId ?: photo.id
            val cacheKey = fileId ?: photo.downloadUrl ?: photo.originalUrl ?: ""

            // 策略 0：優先檢查雙層圖片快取（L1 記憶體 / L2 磁碟）
            if (cacheKey.isNotBlank() && imageCacheManager != null) {
                val cached = imageCacheManager.get(cacheKey)
                if (cached != null) {
                    photo.base64Data = cached.base64Data
                    photo.mimeType = cached.mimeType
                    return@map photo
                }
            }

            // 策略 1：優先透過 GAS 代理下載（DriveApp 後端轉 Base64，完全避開 Google Drive 登入/Cookie 阻擋）
            if (!fileId.isNullOrBlank() && gasUrl.isNotBlank()) {
                try {
                    var proxyUrl = appendParam(gasUrl, "action", "getImageBase64")
                    proxyUrl = "$proxyUrl&fileId=$fileId"
                    val req = Request.Builder().url(proxyUrl).get().build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val respStr = resp.body?.string() ?: ""
                            val imgResp = gson.fromJson(respStr, GasImageResponse::class.java)
                            if (imgResp.success && !imgResp.base64.isNullOrBlank()) {
                                photo.base64Data = imgResp.base64
                                photo.mimeType = imgResp.mimeType ?: "image/jpeg"
                                loaded = true
                                if (cacheKey.isNotBlank() && imageCacheManager != null) {
                                    imageCacheManager.put(cacheKey, photo.base64Data!!, photo.mimeType ?: "image/jpeg")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("GasRepository", "GAS getImageBase64 failed for $fileId", e)
                }
            }

            // 策略 2：若無 fileId 或代理失敗，嘗試直連下載
            if (!loaded) {
                val downloadUrl = photo.downloadUrl ?: photo.originalUrl
                if (!downloadUrl.isNullOrBlank()) {
                    try {
                        val req = Request.Builder().url(downloadUrl).get().build()
                        client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bytes = resp.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    photo.base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    photo.mimeType = resp.header("Content-Type", "image/jpeg")
                                    loaded = true
                                    if (cacheKey.isNotBlank() && imageCacheManager != null) {
                                        imageCacheManager.put(cacheKey, photo.base64Data!!, photo.mimeType ?: "image/jpeg")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("GasRepository", "Direct photo download failed", e)
                    }
                }
            }

            photo
        }
    }

    suspend fun uploadItem(
        gasUrl: String,
        photosBase64: List<String>,
        address: String,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (gasUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("尚未設定 Google Apps Script 網址"))
            }

            val payloadMap = mapOf(
                "action" to "uploadItem",
                "photos" to photosBase64,
                "address" to address,
                "note" to note
            )
            val jsonPayload = gson.toJson(payloadMap)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(gasUrl)
                .post(requestBody)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP 錯誤 ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)
                if (gasResponse.success) {
                    Result.success(Unit)
                } else {
                    val errMsg = gasResponse.error ?: gasResponse.message ?: "拍照上傳失敗"
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("GasRepository", "uploadItem failed", e)
            Result.failure(e)
        }
    }
}


package org.rotary.goodsassistant.data

import android.util.Base64
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.rotary.goodsassistant.model.GasResponse
import org.rotary.goodsassistant.model.GoodsItem
import org.rotary.goodsassistant.model.PhotoItem
import java.util.concurrent.TimeUnit

class GasRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getQueue(gasUrl: String): Result<List<GoodsItem>> = withContext(Dispatchers.IO) {
        try {
            if (gasUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("尚未設定 Google Apps Script 網址"))
            }

            val requestBody = gson.toJson(mapOf("action" to "getItems")).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(gasUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("伺服器回應錯誤: ${response.code}"))
                }
                val bodyStr = response.body?.string() ?: ""
                val gasResponse = gson.fromJson(bodyStr, GasResponse::class.java)

                if (gasResponse.success && gasResponse.data?.items != null) {
                    val pending = gasResponse.data.items.filter { it.status == "待刊登" }
                    Result.success(pending)
                } else {
                    Result.failure(Exception(gasResponse.error ?: "無法獲取佇列資料"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun lockItem(gasUrl: String, row: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf("action" to "lockItem", "row" to row)
            val requestBody = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(gasUrl).post(requestBody).build()
            client.newCall(request).execute().close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markCompleted(gasUrl: String, row: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf("action" to "updateStatus", "row" to row, "status" to "已刊登")
            val requestBody = gson.toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(gasUrl).post(requestBody).build()
            client.newCall(request).execute().close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun preparePhotosBase64(photos: List<PhotoItem>, gasUrl: String): List<PhotoItem> = withContext(Dispatchers.IO) {
        photos.take(3).map { photo ->
            val downloadUrl = photo.downloadUrl ?: photo.originalUrl
            if (!downloadUrl.isNullOrBlank()) {
                try {
                    val req = Request.Builder().url(downloadUrl).build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                photo.base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                photo.mimeType = resp.header("Content-Type", "image/jpeg")
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to fetch via GAS relay if direct Drive link blocked
                    try {
                        val relayPayload = mapOf("action" to "fetchImage", "fileId" to photo.id)
                        val relayReq = Request.Builder()
                            .url(gasUrl)
                            .post(gson.toJson(relayPayload).toRequestBody(jsonMediaType))
                            .build()
                        client.newCall(relayReq).execute().use { relayResp ->
                            val respJson = gson.fromJson(relayResp.body?.string(), Map::class.java)
                            if (respJson["success"] == true) {
                                photo.base64Data = respJson["base64"] as? String
                                photo.mimeType = (respJson["mimeType"] as? String) ?: "image/jpeg"
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            photo
        }
    }
}

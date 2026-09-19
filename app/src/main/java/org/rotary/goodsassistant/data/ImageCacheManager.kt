package org.rotary.goodsassistant.data

import android.content.Context
import android.util.Base64
import android.util.Log
import android.util.LruCache
import java.io.File
import java.security.MessageDigest

class ImageCacheManager(private val context: Context) {

    // L1: 記憶體快取 (最多保留 15 張照片)
    private val memoryCache = LruCache<String, CachedImage>(15)

    // L2: 磁碟快取目錄
    private val diskCacheDir: File
        get() = File(context.cacheDir, "goods_photos").apply {
            if (!exists()) mkdirs()
        }

    data class CachedImage(
        val base64Data: String,
        val mimeType: String
    )

    private fun hashKey(key: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(key.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            key.replace("[^a-zA-Z0-9]".toRegex(), "_")
        }
    }

    /**
     * 嘗試自快取（L1 記憶體 或 L2 磁碟）取得照片數據
     */
    fun get(key: String): CachedImage? {
        if (key.isBlank()) return null
        val safeKey = hashKey(key)

        // 1. 檢查 L1 記憶體快取
        val memItem = memoryCache.get(safeKey)
        if (memItem != null) {
            Log.d("ImageCacheManager", "⚡ 命中 L1 記憶體圖片快取: $key")
            return memItem
        }

        // 2. 檢查 L2 磁碟快取
        val diskFile = File(diskCacheDir, "img_$safeKey.dat")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bytes = diskFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val cached = CachedImage(base64, "image/jpeg")
                memoryCache.put(safeKey, cached)
                Log.d("ImageCacheManager", "💾 命中 L2 磁碟圖片快取: $key")
                return cached
            } catch (e: Exception) {
                Log.w("ImageCacheManager", "讀取磁碟圖片失敗", e)
            }
        }

        return null
    }

    /**
     * 存入快取（同時寫入 L1 與 L2）
     */
    fun put(key: String, base64Data: String, mimeType: String = "image/jpeg") {
        if (key.isBlank() || base64Data.isBlank()) return
        val safeKey = hashKey(key)

        val cached = CachedImage(base64Data, mimeType)
        memoryCache.put(safeKey, cached)

        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val diskFile = File(diskCacheDir, "img_$safeKey.dat")
            diskFile.writeBytes(bytes)
        } catch (e: Exception) {
            Log.w("ImageCacheManager", "寫入磁碟圖片快取失敗", e)
        }
    }

    /**
     * 清理所有圖片快取
     */
    fun clear() {
        memoryCache.evictAll()
        try {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.w("ImageCacheManager", "清除磁碟圖片快取失敗", e)
        }
    }
}

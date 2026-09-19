package org.rotary.goodsassistant.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import org.rotary.goodsassistant.model.GoodsItem
import java.io.File

data class QueueCacheData(
    val timestamp: Long = 0L,
    val items: List<GoodsItem> = emptyList()
)

class QueueCacheManager(private val context: Context) {
    private val gson = Gson()
    private val cacheFile: File
        get() = File(context.cacheDir, "queue_cache.json")

    companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L // 快取有效期限：5 分鐘
    }

    /**
     * 讀取本地快取清單與時間戳
     */
    fun loadQueue(): Pair<List<GoodsItem>, Long>? {
        return try {
            val file = cacheFile
            if (!file.exists()) return null
            val json = file.readText()
            if (json.isBlank()) return null
            val cacheData = gson.fromJson(json, QueueCacheData::class.java)
            if (cacheData != null && cacheData.items.isNotEmpty()) {
                Pair(cacheData.items, cacheData.timestamp)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("QueueCacheManager", "讀取快取失敗", e)
            null
        }
    }

    /**
     * 儲存清單至快取檔案
     */
    fun saveQueue(items: List<GoodsItem>) {
        try {
            val data = QueueCacheData(
                timestamp = System.currentTimeMillis(),
                items = items
            )
            val json = gson.toJson(data)
            cacheFile.writeText(json)
            Log.d("QueueCacheManager", "已快取 ${items.size} 筆物資")
        } catch (e: Exception) {
            Log.e("QueueCacheManager", "儲存快取失敗", e)
        }
    }

    /**
     * 更新快取中特定 row 的狀態（如「刊登中」或「待刊登」）
     */
    fun updateItemStatus(row: Int, newStatus: String) {
        val cached = loadQueue() ?: return
        val updated = cached.first.map { item ->
            if (item.row == row) item.copy(status = newStatus) else item
        }
        saveQueue(updated)
    }

    /**
     * 從快取中移除特定 row 的物資（如刪除或完成刊登）
     */
    fun removeItem(row: Int) {
        val cached = loadQueue() ?: return
        val filtered = cached.first.filter { it.row != row }
        saveQueue(filtered)
    }

    /**
     * 清除快取
     */
    fun clearCache() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (e: Exception) {
            Log.e("QueueCacheManager", "清除快取失敗", e)
        }
    }
}

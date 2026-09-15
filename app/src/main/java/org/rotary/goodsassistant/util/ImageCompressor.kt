package org.rotary.goodsassistant.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

data class CompressedPhoto(
    val bitmap: Bitmap,
    val base64: String,
    val mimeType: String = "image/jpeg"
)

object ImageCompressor {

    suspend fun compressUri(context: Context, uri: Uri, maxDimension: Int = 1600, quality: Int = 80): CompressedPhoto? = withContext(Dispatchers.IO) {
        try {
            // 1. 取得 EXIF 旋轉方向
            var orientation = ExifInterface.ORIENTATION_NORMAL
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }

            // 2. 先解碼圖片邊界大小以計算 InSampleSize
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            var inSampleSize = 1
            val maxEdge = max(origWidth, origHeight)
            if (maxEdge > maxDimension) {
                inSampleSize = Math.round(maxEdge.toFloat() / maxDimension.toFloat())
            }

            // 3. 實際解碼採樣後的 Bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = max(1, inSampleSize)
                this.inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { stream ->
                bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (bitmap == null) return@withContext null

            // 4. 根據 EXIF 方向進行旋轉
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            // 5. 若尺寸仍略大於 maxDimension，進行精確縮放
            val currentMax = max(bitmap!!.width, bitmap!!.height)
            if (currentMax > maxDimension) {
                val scale = maxDimension.toFloat() / currentMax.toFloat()
                matrix.postScale(scale, scale)
            }

            val finalBitmap = Bitmap.createBitmap(
                bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true
            )

            // 6. 壓縮為 JPEG
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            CompressedPhoto(
                bitmap = finalBitmap,
                base64 = base64,
                mimeType = "image/jpeg"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressionHelper {

    suspend fun compressUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        val maxSide = max(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (maxSide / sampleSize > 1024) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: return@withContext null

        val targetMaxDim = 540
        val scale = if (decoded.width > targetMaxDim || decoded.height > targetMaxDim) {
            targetMaxDim.toFloat() / max(decoded.width, decoded.height).toFloat()
        } else {
            1f
        }
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        var quality = 78
        var jpegBytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            jpegBytes = out.toByteArray()
            quality -= 12
        } while (jpegBytes.size > 90_000 && quality >= 30)

        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()

        Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
    }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}

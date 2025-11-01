package com.localchat.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageCompressor {

    /**
     * Compress image from URI
     * - Load bitmap from URI using ContentResolver
     * - Scale down if width > 800px (maintain aspect ratio)
     * - Compress to JPEG with 80% quality
     * - Return ByteArray
     */
    fun compressImage(context: Context, uri: Uri, maxWidth: Int = 800, quality: Int = 80): ByteArray? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return null
            }

            // Scale down if needed
            val scaledBitmap = if (originalBitmap.width > maxWidth) {
                val scaleFactor = maxWidth.toFloat() / originalBitmap.width
                val newHeight = (originalBitmap.height * scaleFactor).toInt()
                Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)
            } else {
                originalBitmap
            }

            // Compress to JPEG
            val byteArray = bitmapToByteArray(scaledBitmap, quality)

            // Clean up if we created a scaled version
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()

            return byteArray
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Convert bitmap to byte array with specified quality
     * Compress to JPEG format
     */
    fun bitmapToByteArray(bitmap: Bitmap, quality: Int = 80): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Convert byte array to bitmap
     * Decode from byte array
     */
    fun byteArrayToBitmap(bytes: ByteArray): Bitmap? {
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

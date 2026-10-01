package com.taka.silentcamera.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import androidx.core.graphics.createBitmap

fun saveImage(context: Context, imageProxy: ImageProxy) {
    val buffer = imageProxy.planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val width = imageProxy.width
    val height = imageProxy.height
    val bitmap = createBitmap(width, height)
    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))

    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val rotatedBitmap = if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true).also {
            if (it != bitmap) bitmap.recycle()
        }
    } else {
        bitmap
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "SilentCamera_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SilentCamera")
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
    }

    rotatedBitmap.recycle()
}
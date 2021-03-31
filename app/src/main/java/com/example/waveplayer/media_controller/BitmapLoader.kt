package com.example.waveplayer.media_controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

object BitmapLoader {
    fun getThumbnail(uri: Uri?, width: Int, height: Int, context: Context?): Bitmap? {
        var bitmap: Bitmap? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                bitmap = context.getContentResolver().loadThumbnail(
                        uri, Size(width, height), null)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            val mmr: MediaMetadataRetriever? = MediaMetadataRetriever()
            try {
                mmr.setDataSource(context.getContentResolver().openFileDescriptor(
                        uri, "r").getFileDescriptor())
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            var inputStream: InputStream? = null
            if (mmr.getEmbeddedPicture() != null) {
                inputStream = ByteArrayInputStream(mmr.getEmbeddedPicture())
            }
            mmr.release()
            bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                return getResizedBitmap(bitmap, width, height)
            }
        }
        return bitmap
    }

    fun getResizedBitmap(bm: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        val width: Int = bm.getWidth()
        val height: Int = bm.getHeight()
        val scaleWidth: Float = (newWidth as Float) / width
        val scaleHeight: Float = (newHeight as Float) / height
        val matrix: Matrix? = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true)
    }
}
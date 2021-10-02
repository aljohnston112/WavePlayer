package com.fourthFinger.pinkyPlayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class BitmapUtil {

    companion object {

        fun getThumbnailBitmap(audioUri: AudioUri?,
                               songArtWidth : Int,
                               context: Context
        ): Bitmap? {
            return audioUri?.id?.let { id ->
                getThumbnail(
                    AudioUri.getUri(id), songArtWidth,
                    songArtWidth,
                    context
                )
            }
        }

        fun getThumbnail(uri: Uri, width: Int, height: Int, context: Context): Bitmap? {
            var bitmap: Bitmap? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    bitmap = context.contentResolver.loadThumbnail(
                        uri,
                        Size(width, height),
                        null)

                } catch (e: FileNotFoundException) {

                }
            } else {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(context.contentResolver.openFileDescriptor(
                        uri,
                        "r")
                        ?.fileDescriptor)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
                var inputStream: InputStream? = null
                if (mmr.embeddedPicture != null) {
                    inputStream = ByteArrayInputStream(mmr.embeddedPicture)
                }
                mmr.release()
                bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    return Bitmap.createScaledBitmap(bitmap, width, height, true)
                }
            }
            return bitmap
        }

        fun getDefaultBitmap(songArtWidth: Int, context: Context): Bitmap? {
            // TODO cache bitmap
            if (songArtWidth > 0 && songArtWidth > 0) {
                val drawableSongArt: Drawable? = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.music_note_black_48dp,
                    context.theme
                )
                if (drawableSongArt != null) {
                    return drawableSongArt.toBitmap(songArtWidth, songArtWidth)
                }
            }
            return null
        }

    }

}
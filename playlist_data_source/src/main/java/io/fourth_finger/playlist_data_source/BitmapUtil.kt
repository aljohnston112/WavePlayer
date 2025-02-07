package io.fourth_finger.playlist_data_source

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
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class BitmapUtil {

    companion object {

        fun getThumbnailBitmap(
            context: Context,
            audioUri: AudioUri?,
            songArtWidth: Int
        ): Bitmap? {
            return audioUri?.id?.let { id ->
                getThumbnail(
                    context,
                    AudioUri.getUri(id), songArtWidth,
                    songArtWidth
                )
            }
        }

        fun getThumbnail(
            context: Context,
            uri: Uri,
            width: Int,
            height: Int
        ): Bitmap? {
            var bitmap: Bitmap?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                bitmap = try {
                    context.contentResolver.loadThumbnail(
                        uri,
                        Size(width, height),
                        null
                    )

                } catch (e: FileNotFoundException) {
                    null
                }
            } else {
                val mmr = MediaMetadataRetriever()
                try {
                    context.contentResolver.openFileDescriptor(
                        uri,
                        "r"
                    )?.use {
                        mmr.setDataSource(it.fileDescriptor)
                    }
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

        fun getDefaultBitmap(
            context: Context,
            songArtWidth: Int
        ): Bitmap? {
            // TODO cache bitmap
            if (songArtWidth > 0) {
                val drawableSongArt: Drawable? = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.music_note_black_48dp,
                    context.theme
                )
                if (drawableSongArt != null) {
                    return drawableSongArt.toBitmap(
                        songArtWidth,
                        songArtWidth
                    )
                }
            }
            return null
        }

    }

}
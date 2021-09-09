package com.fourthFinger.pinkyPlayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.fourthFinger.pinkyPlayer.media_controller.BitmapLoader
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri

class BitmapUtil {
    companion object {

        fun getBitmap(audioUri: AudioUri?,
                      songArtWidth : Int,
                      context: Context
        ): Bitmap? {
            return audioUri?.id?.let { id ->
                AudioUri.getUri(id).let {
                    BitmapLoader.getThumbnail(
                        it, songArtWidth,
                        songArtWidth,
                        context
                    )
                }
            }
        }

        fun getDefaultBitmap(songArtWidth: Int, context: Context): Bitmap? {
            // TODO cache bitmap
            if (songArtWidth > 0 && songArtWidth > 0) {
                val drawableSongArt: Drawable? = ResourcesCompat.getDrawable(
                    context.resources, R.drawable.music_note_black_48dp, context.theme
                )
                if (drawableSongArt != null) {
                    drawableSongArt.setBounds(0, 0, songArtWidth, songArtWidth)
                    val bitmapSongArt: Bitmap = Bitmap.createBitmap(
                        songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmapSongArt)
                    drawableSongArt.draw(canvas)
                    return bitmapSongArt
                }
            }
            return null
        }

    }

}
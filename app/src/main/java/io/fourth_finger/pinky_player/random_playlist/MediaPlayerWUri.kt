package io.fourth_finger.pinky_player.random_playlist

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.os.Build
import io.fourth_finger.playlist_data_source.AudioUri
import java.util.*

class MediaPlayerWUri(
    private val mediaPlayerManager: MediaPlayerManager,
    private var mediaPlayer: MediaPlayer,
    val id: Long
) {

    @Volatile
    private var isPrepared: Boolean = false
    fun isPrepared(): Boolean {
        synchronized(lock) { return isPrepared }
    }

    @Volatile
    private var shouldPlay: Boolean = false

    fun shouldPlay(shouldPlay: Boolean) {
        synchronized(lock) {
            if (shouldPlay && isPrepared) {
                mediaPlayer.start()
            } else {
                this.shouldPlay = shouldPlay
            }
        }
    }

    fun release() {
        synchronized(lock) {
            mediaPlayer.setOnCompletionListener(null)
            isPrepared = false
            shouldPlay = false
            mediaPlayer.reset()
            mediaPlayer.release()
        }
    }

    fun prepareAsync() {
        synchronized(lock) {
            isPrepared = false
            mediaPlayer.prepareAsync()
        }
    }

    fun getCurrentPosition(): Int {
        synchronized(lock) {
            return if (isPrepared) {
                mediaPlayer.currentPosition
            } else {
                -1
            }
        }
    }

    fun isPlaying(): Boolean {
        synchronized(lock) {
            return if (isPrepared) {
                mediaPlayer.isPlaying
            } else {
                false
            }
        }
    }

    fun stop(
        context: Context,
        audioUri: AudioUri
    ) {
        synchronized(lock) {
            isPrepared = false
            shouldPlay = false
            mediaPlayer.stop()
            resetIfMKV(audioUri, context)
        }
    }

    fun pause() {
        synchronized(lock) {
            if (isPrepared) {
                mediaPlayer.pause()
            }
            shouldPlay = false
        }
    }

    fun seekTo(
        context: Context,
        audioUri: AudioUri,
        millis: Int
    ) {
        synchronized(lock) {
            if (isPrepared) {
                if (resetIfMKV(audioUri, context)) {
                    shouldPlay = true
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer.seekTo(millis.toLong(), MediaPlayer.SEEK_CLOSEST)
                } else {
                    mediaPlayer.seekTo(millis)
                }
            }
        }
    }

    private fun resetIfMKV(
        audioUri: AudioUri,
        context: Context
    ): Boolean {
        val s: Array<String> = audioUri.displayName.split("\\.").toTypedArray()
        if (s.isNotEmpty()) {
            if ((s[s.size - 1].lowercase(Locale.ROOT) == "mkv")) {
                mediaPlayer.reset()
                mediaPlayer.release()
                mediaPlayer = MediaPlayer.create(context, audioUri.getUri())
                mediaPlayer.setOnPreparedListener(null)
                mediaPlayer.setOnErrorListener(null)
                mediaPlayer.setOnPreparedListener(mOnPreparedListener)
                mediaPlayer.setOnErrorListener(mediaPlayerManager.onErrorListener)
                mediaPlayer.setOnCompletionListener(mediaPlayerManager.onCompletionListener)
                return true
            }
        }
        return false
    }

    private val mOnPreparedListener = OnPreparedListener { mediaPlayer ->
        synchronized(this) {
            isPrepared = true
            if (shouldPlay) {
                mediaPlayer.setOnCompletionListener(mediaPlayerManager.onCompletionListener)
                mediaPlayer.start()
                shouldPlay = false
            }
        }
    }

    companion object {
        val lock: Any = Any()
    }

    init {
        mediaPlayer.setOnPreparedListener(null)
        mediaPlayer.setOnErrorListener(null)
        mediaPlayer.setOnPreparedListener(mOnPreparedListener)
        mediaPlayer.setOnErrorListener(mediaPlayerManager.onErrorListener)
    }
}
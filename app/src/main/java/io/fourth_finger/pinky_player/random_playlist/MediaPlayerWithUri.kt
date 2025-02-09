package io.fourth_finger.pinky_player.random_playlist

import io.fourth_finger.playlist_data_source.AudioUri

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.os.Build

import java.util.Locale

class MediaPlayerWithUri(
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

    private val mOnPreparedListener = OnPreparedListener { mediaPlayer ->
        synchronized(this) {
            isPrepared = true
            if (shouldPlay) {
                mediaPlayer.setOnCompletionListener(mediaPlayerManager.onCompletionListener)
                mediaPlayer.start()
                shouldPlay = false
                mediaPlayerManager.setIsPlaying(true)
                mediaPlayerManager.setSongInProgress(true)
            }
        }
    }

    private fun initMediaPlayerListeners() {
        mediaPlayer.setOnPreparedListener(null)
        mediaPlayer.setOnErrorListener(null)
        mediaPlayer.setOnPreparedListener(mOnPreparedListener)
        mediaPlayer.setOnErrorListener(mediaPlayerManager.onErrorListener)
    }

    init {
        initMediaPlayerListeners()
    }

    fun prepareAsync() {
        synchronized(lock) {
            isPrepared = false
            mediaPlayer.prepareAsync()
        }
    }

    fun shouldPlay(shouldPlay: Boolean) {
        synchronized(lock) {
            if (shouldPlay && isPrepared) {
                mediaPlayer.start()
                mediaPlayerManager.setIsPlaying(true)
                mediaPlayerManager.setSongInProgress(true)
            } else {
                this.shouldPlay = shouldPlay
            }
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

    fun isPlaying(): Boolean {
        synchronized(lock) {
            return if (isPrepared) {
                mediaPlayer.isPlaying
            } else {
                false
            }
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

    /**
     * There was a bug when this was created where MKV files would not properly seek
     */
    private fun resetIfMKV(
        context: Context,
        audioUri: AudioUri
    ): Boolean {
        val s: Array<String> = audioUri.displayName.split("\\.").toTypedArray()
        if (s.isNotEmpty()) {
            if ((s[s.size - 1].lowercase(Locale.ROOT) == "mkv")) {
                releaseMediaPlayer()
                mediaPlayer = MediaPlayer.create(
                    context,
                    audioUri.getUri()
                )
                initMediaPlayerListeners()
                mediaPlayer.setOnCompletionListener(mediaPlayerManager.onCompletionListener)
                return true
            }
        }
        return false
    }

    fun seekTo(
        context: Context,
        audioUri: AudioUri,
        millis: Int
    ) {
        synchronized(lock) {
            if (isPrepared) {
                if (resetIfMKV(context, audioUri)) {
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

    fun stop(
        context: Context,
        audioUri: AudioUri
    ) {
        synchronized(lock) {
            isPrepared = false
            shouldPlay = false
            mediaPlayer.stop()
            resetIfMKV(context, audioUri)
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer.setOnPreparedListener(null)
        mediaPlayer.setOnErrorListener(null)
        mediaPlayer.setOnCompletionListener(null)
        mediaPlayer.reset()
        mediaPlayer.release()
    }

    fun release() {
        synchronized(lock) {
            isPrepared = false
            shouldPlay = false
            releaseMediaPlayer()
        }
    }

    companion object {
        val lock: Any = Any()
    }

}
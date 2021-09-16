package com.fourthFinger.pinkyPlayer.media_controller

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import java.util.*

class MediaPlayerSession private constructor() {

    private var _currentAudioUri = MutableLiveData<AudioUri>()
    val currentAudioUri = _currentAudioUri as LiveData<AudioUri>
    fun setCurrentAudioUri(audioUri: AudioUri) {
        _currentAudioUri.value = audioUri
    }

    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying = _isPlaying as LiveData<Boolean>
    fun setIsPlaying(isPlaying: Boolean) {
        this._isPlaying.setValue(isPlaying)
    }

    fun getCurrentAudioUri(): AudioUri? {
        return _currentAudioUri.value
    }

    fun getCurrentUri(): Uri? {
        return _currentAudioUri.value?.getUri()
    }

    fun getCurrentTime(): Int {
        return getCurrentMediaPlayerWUri()?.getCurrentPosition() ?: -1
    }

    private val songIDToMediaPlayerWUriHashMap: HashMap<Long, MediaPlayerWUri> = HashMap()
    fun getMediaPlayerWUri(songID: Long): MediaPlayerWUri? {
        return songIDToMediaPlayerWUriHashMap[songID]
    }

    fun addMediaPlayerWUri(mediaPlayerWURI: MediaPlayerWUri) {
        songIDToMediaPlayerWUriHashMap[mediaPlayerWURI.id] = mediaPlayerWURI
    }

    fun getCurrentMediaPlayerWUri(): MediaPlayerWUri? {
        return _currentAudioUri.value?.id?.let { getMediaPlayerWUri(it) }
    }

    fun removeMediaPlayerWUri(songID: Long) {
        songIDToMediaPlayerWUriHashMap.remove(songID)
    }

    fun releaseMediaPlayers(){
        synchronized(this) {
            for (mediaPlayerWURI in songIDToMediaPlayerWUriHashMap.values) {
                mediaPlayerWURI.release()
            }
            songIDToMediaPlayerWUriHashMap.clear()
        }
        _isPlaying.value = false
    }

    fun taskRemoved(context: Context) {
        if (isPlaying.value == true) {
            val mediaSession = MediaSession.getInstance(context)
            mediaSession.pauseOrPlay(context)
        }
        releaseMediaPlayers()
    }

    companion object{

        private var INSTANCE: MediaPlayerSession? = null

        fun getInstance(): MediaPlayerSession {
            if(INSTANCE == null){
                INSTANCE = MediaPlayerSession()
            }
            return INSTANCE!!
        }
    }

}
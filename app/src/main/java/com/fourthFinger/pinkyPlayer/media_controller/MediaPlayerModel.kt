package com.fourthFinger.pinkyPlayer.media_controller

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fourthFinger.pinkyPlayer.fragments.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import java.util.HashMap

class MediaPlayerModel private constructor() {

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

    companion object{

        private var INSTANCE: MediaPlayerModel? = null

        fun getInstance(): MediaPlayerModel {
            if(INSTANCE == null){
                INSTANCE = MediaPlayerModel()
            }
            return INSTANCE!!
        }
    }

}
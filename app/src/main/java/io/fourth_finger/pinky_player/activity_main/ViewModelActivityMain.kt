package io.fourth_finger.pinky_player.activity_main

import android.content.Context
import android.os.Bundle
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import io.fourth_finger.pinky_player.ApplicationMain
import io.fourth_finger.pinky_player.NavUtil
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.fragments.FragmentPlaylistDirections
import io.fourth_finger.pinky_player.fragments.FragmentSongsDirections
import io.fourth_finger.pinky_player.random_playlist.MediaSession
import io.fourth_finger.pinky_player.random_playlist.SongRepo
import io.fourth_finger.pinky_player.random_playlist.UseCaseEditPlaylist
import io.fourth_finger.pinky_player.settings.SettingsRepo
import io.fourth_finger.playlist_data_source.PlaylistsRepo
import io.fourth_finger.playlist_data_source.RandomPlaylist
import io.fourth_finger.playlist_data_source.Song
import java.util.Locale

class ViewModelActivityMain(
    private val settingsRepo: SettingsRepo,
    private val mediaSession: MediaSession,
    private val playlistEditor: UseCaseEditPlaylist,
    private val playlistsRepo: PlaylistsRepo,
    private val songRepo: SongRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val currentlyPlayingPlaylist = mediaSession.currentlyPlayingPlaylist
    val currentAudioUri = mediaSession.currentAudioUri
    val isPlaying = mediaSession.isPlaying
    val songInProgress = mediaSession.songInProgress

    var currentContextPlaylist: RandomPlaylist? = null
        set(playlist){
            field = playlist
        }

    private val _actionBarTitle: MutableLiveData<String> = MutableLiveData()
    val actionBarTitle = _actionBarTitle as LiveData<String>
    fun setActionBarTitle(actionBarTitle: String) {
        this._actionBarTitle.postValue(actionBarTitle)
    }

    private val _showFab: MutableLiveData<Boolean> = MutableLiveData(false)
    val showFab = _showFab as LiveData<Boolean>
    fun showFab(showFAB: Boolean) {
        this._showFab.postValue(showFAB)
    }

    private val _fabText: MutableLiveData<Int> = MutableLiveData()
    val fabText = _fabText as LiveData<Int>
    fun setFABText(@StringRes fabText: Int) {
        this._fabText.postValue(fabText)
    }

    private val _fabImageID: MutableLiveData<Int> = MutableLiveData()
    val fabImageID = _fabImageID as LiveData<Int>
    fun setFabImage(@DrawableRes fabImageID: Int) {
        this._fabImageID.postValue(fabImageID)
    }

    private val _fabOnClickListener: MutableLiveData<OnClickListener> = MutableLiveData()
    val fabOnClickListener =_fabOnClickListener as LiveData<OnClickListener>
    fun setFabOnClickListener(fabOnClickListener: OnClickListener?) {
        this._fabOnClickListener.postValue(fabOnClickListener)
    }

    private val _songPaneVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val songPaneVisible = _songPaneVisible as LiveData<Boolean>
    fun songPaneVisible(visibility: Int) {
        this._songPaneVisible.postValue(visibility == VISIBLE)
    }

    private val _fragmentSongVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val fragmentSongVisible = _fragmentSongVisible as LiveData<Boolean>
    fun fragmentSongVisible(visible: Boolean) {
        this._fragmentSongVisible.postValue(visible)
    }

    fun lowerProbabilities(context: Context) {
        mediaSession.lowerProbabilities(
            context,
            settingsRepo.settings.value!!.lowerProb
        )
    }

    fun resetProbabilities(context: Context) {
        mediaSession.resetProbabilities(context)
    }

    fun notifySongMoved(
        context: Context,
        from: Int,
        to: Int
    ) {
        playlistEditor.notifySongMoved(context, from, to)
    }

    fun notifySongRemoved(fragment: Fragment, position: Int) {
        playlistEditor.notifySongRemoved(fragment, position)
    }

    fun notifyItemInserted(context: Context, position: Int) {
        playlistEditor.notifyItemInserted(context, position)
    }

    // TODO add setting to stop playlist from continuing after queue is done
    // shuffle is off and looping is on or something like that?

    private val _playlistToAddToQueue = MutableLiveData<RandomPlaylist?>()
    private val playlistToAddToQueue = _playlistToAddToQueue as LiveData<RandomPlaylist?>
    private fun getPlaylistToAddToQueue(): RandomPlaylist? {
        return playlistToAddToQueue.value?.let { playlistsRepo.getPlaylist(it.name) }
    }
    fun setPlaylistToAddToQueue(playlistToAddToQueue: RandomPlaylist?) {
        _playlistToAddToQueue.postValue(playlistToAddToQueue)
        _songToAddToQueue.postValue(null)
    }

    private val _songToAddToQueue = MutableLiveData<Long?>()
    private val songToAddToQueue = _songToAddToQueue as LiveData<Long?>
    private fun getSongToAddToQueue(): Song? {
        return songToAddToQueue.value?.let { songRepo.getSong(it) }
    }
    fun setSongToAddToQueue(songToAddToQueue: Long?) {
        _songToAddToQueue.postValue(songToAddToQueue)
        _playlistToAddToQueue.postValue(null)
    }

    fun addToQueue(context: Context, song: Song) {
        mediaSession.addToQueueAndStartIfNeeded(context, song.id)
    }

    fun addToQueue(context: Context, randomPlaylist: RandomPlaylist) {
        mediaSession.addToQueueAndStartIfNeeded(context, randomPlaylist)
    }

    fun actionAddToQueue(context: Context) {
        // TODO song and playlist could be non-null at the same time
        // TODO Song will play even though user might not want it. Make a setting.
        songToAddToQueue.value?.let { mediaSession.addToQueueAndStartIfNeeded(context, it) }
        playlistToAddToQueue.value?.let {
            mediaSession.addToQueueAndStartIfNeeded(context, it)
        }
    }

    fun actionAddToPlaylist(supportFragmentManager: FragmentManager) {
        val bundle = Bundle()
        getSongToAddToQueue()?.let {
            bundle.putSerializable(
                DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG,
                it
            )
        }
        getPlaylistToAddToQueue()?.let {
            bundle.putSerializable(
                DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST,
                it
            )
        }
        val dialogFragment: DialogFragment = DialogFragmentAddToPlaylist()
        dialogFragment.arguments = bundle
        dialogFragment.show(supportFragmentManager, DialogFragmentAddToPlaylist.TAG)
    }

    fun queueSongClicked(
        fragment: Fragment,
        queuePosition: Int
    ) {
        synchronized(io.fourth_finger.pinky_player.activity_main.ActivityMain.Companion.MUSIC_CONTROL_LOCK) {
            val context = fragment.requireContext()
            mediaSession.startFromIndex(context, queuePosition)
        }
        NavUtil.navigateTo(fragment, R.id.fragmentSong)
    }


    fun playlistSongClicked(
        fragment: Fragment,
        song: Song
    ) {
        val context = fragment.requireContext()
        val navController = fragment.findNavController()
        synchronized(io.fourth_finger.pinky_player.activity_main.ActivityMain.Companion.MUSIC_CONTROL_LOCK) {
            // TODO pass current song in?
            if (song == mediaSession.currentAudioUri.value?.id?.let {
                    songRepo.getSong(it)
                }
            ) {
                mediaSession.seekTo(context, 0)
            }
            if(navController.currentDestination?.id == R.id.fragmentSongs){
                mediaSession.setCurrentPlaylistToMaster()
            }
            mediaSession.startPlaylistFromSong(context, song.id)
        }
        if (navController.currentDestination?.id == R.id.fragmentPlaylist) {
            NavUtil.navigate(
                navController,
                FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong()
            )
        } else if (navController.currentDestination?.id == R.id.fragmentSongs) {
            NavUtil.navigate(
                navController,
                FragmentSongsDirections.actionFragmentSongsToFragmentSong()
            )
        }
    }

    fun playPauseClicked(context: Context) {
        mediaSession.pauseOrPlay(context)
    }

    fun playNext(context: Context) {
        mediaSession.playNext(context)
    }

    fun getAllSongs(): List<Song> {
        return songRepo.getAllSongs()
    }

    fun siftAllSongs(newText: String): List<Song> {
        val sifted: MutableList<Song> = mutableListOf()
        for (song in getAllSongs()) {
            if (song.title.lowercase(Locale.ROOT)
                    .contains(newText.lowercase(Locale.ROOT))
            ) {
                sifted.add(song)
            }
        }
        return sifted
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])
                // Create a SavedStateHandle for this ViewModel from extras
                val savedStateHandle = extras.createSavedStateHandle()

                return ViewModelActivityMain(
                    (application as ApplicationMain).settingsRepo!!,
                    application.mediaSession!!,
                    application.playlistEditor!!,
                    application.playlistsRepo!!,
                    application.songRepo!!,
                    savedStateHandle
                ) as T
            }
        }
    }

}
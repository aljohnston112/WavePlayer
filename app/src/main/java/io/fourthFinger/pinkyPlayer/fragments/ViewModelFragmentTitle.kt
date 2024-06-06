package io.fourthFinger.pinkyPlayer.fragments

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import io.fourthFinger.pinkyPlayer.ApplicationMain
import io.fourthFinger.pinkyPlayer.NavUtil
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import io.fourthFinger.pinkyPlayer.settings.SettingsRepo
import io.fourthFinger.playlistDataSource.PlaylistsRepo
import io.fourthFinger.playlistDataSource.RandomPlaylist
import io.fourthFinger.playlistDataSource.Song


class ViewModelFragmentTitle(
    private val settingsRepo: SettingsRepo,
    private val playlistsRepo: PlaylistsRepo,
    private val mediaSession: MediaSession,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // TODO add loading progress

    fun buttonClicked(id: Int, navController: NavController) {
        when (id) {
            R.id.button_playlists -> {
                NavUtil.navigate(
                    navController,
                    FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists()
                )
            }

            R.id.button_songs -> {
                NavUtil.navigate(
                    navController,
                    FragmentTitleDirections.actionFragmentTitleToFragmentSongs()
                )
            }

            R.id.button_settings -> {
                NavUtil.navigate(
                    navController,
                    FragmentTitleDirections.actionFragmentTitleToFragmentSettings()
                )
            }
        }
    }

    fun createPlaylistFromFolder(
        fragmentTitle: FragmentTitle,
        uri: Uri
    ) {
        val context = fragmentTitle.requireContext()
        val contentResolver = fragmentTitle.requireContext().contentResolver
        val songs = getFilesFromDirRecursive(contentResolver, uri)
        var randomPlaylist: RandomPlaylist? = uri.path?.let {
            playlistsRepo.getPlaylist(it)
        }
        if (randomPlaylist == null) {
            randomPlaylist = uri.path?.let {
                RandomPlaylist(
                    it,
                    songs,
                    false,
                    settingsRepo.settings.value!!.maxPercent
                )
            }
            if (randomPlaylist != null) {
                playlistsRepo.addPlaylist(context, randomPlaylist)
            }
        } else {
            addNewSongs(context, randomPlaylist, songs)
            removeMissingSongs(context, randomPlaylist, songs)
        }
        if (randomPlaylist != null) {
            NavUtil.navigate(
                fragmentTitle.findNavController(),
                FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists()
            )
        } else{
            // TODO The uri path was null
        }
    }

    private fun getFilesFromDirRecursive(
        contentResolver: ContentResolver,
        rootUri: Uri
    ): MutableList<Song> {
        val childUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
            rootUri,
            DocumentsContract.getTreeDocumentId(rootUri)
        )
        return searchFilesForMusic(contentResolver, childUri, rootUri)
    }

    private fun searchFilesForMusic(
        contentResolver: ContentResolver,
        childUri: Uri,
        rootUri: Uri
    ): MutableList<Song> {
        val songs: MutableList<Song> = mutableListOf()
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != ? OR" +
                DocumentsContract.Document.COLUMN_MIME_TYPE + " " + "== ?"
        val selectionArgs = arrayOf("0", DocumentsContract.Document.MIME_TYPE_DIR)
        contentResolver.query(
            childUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            selection,
            selectionArgs,
            null
        ).use { cursor ->
            if (cursor != null) {
                val docIdCol: Int = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                )
                val mimeCol: Int = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                val nameCol: Int = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )
                while (cursor.moveToNext()) {
                    val docId: String = cursor.getString(docIdCol)
                    val mime: String = cursor.getString(mimeCol)
                    val displayName: String = cursor.getString(nameCol)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val newNode: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                            rootUri,
                            docId
                        )
                        songs.addAll(
                            searchFilesForMusic(contentResolver, newNode, rootUri)
                        )
                    } else {
                        getSong(contentResolver, displayName)?.let {
                            songs.add(it)
                        }
                    }
                }
            }
        }
        return songs
    }

    private fun getSong(
        contentResolver: ContentResolver,
        displayName: String
    ): Song? {
        val selection = MediaStore.Audio.Media.DISPLAY_NAME + " == ?"
        val selectionArgs = arrayOf(displayName)
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            ),
            selection,
            selectionArgs,
            null
        ).use { cursor ->
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)
                    return Song(id, title)
                }
            }
        }
        return null
    }

    private fun addNewSongs(
        context: Context,
        randomPlaylist: RandomPlaylist,
        songs: List<Song>
    ) {
        for (song in songs) {
            if (!randomPlaylist.contains(song.id)) {
                playlistsRepo.addSong(
                    context,
                    randomPlaylist,
                    song
                )
            }
        }
    }

    private fun removeMissingSongs(
        context: Context,
        randomPlaylist: RandomPlaylist,
        songs: MutableList<Song>
    ) {
        for (song in randomPlaylist.getSongs()) {
            if (!songs.contains(song)) {
                playlistsRepo.removeSong(
                    context,
                    randomPlaylist,
                    song
                )
                songs.remove(song)
            }
        }
    }

    fun viewCreated() {
        mediaSession.setCurrentPlaylistToMaster()
    }

    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application =
                    checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val savedStateHandle = extras.createSavedStateHandle()
                return ViewModelFragmentTitle(
                    (application as ApplicationMain).settingsRepo!!,
                    application.playlistsRepo!!,
                    application.mediaSession!!,
                    savedStateHandle
                ) as T
            }
        }

    }

}
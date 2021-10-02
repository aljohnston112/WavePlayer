package com.fourthFinger.pinkyPlayer.fragments

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.random_playlist.PlaylistsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song


class ViewModelFragmentTitle : ViewModel() {

    private var songs: MutableList<Song> = mutableListOf()

    fun clearSongs() {
        songs.clear()
    }

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

    fun createPlaylist(
        context: Context,
        contentResolver: ContentResolver,
        uri: Uri
    ): RandomPlaylist? {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        getFilesFromDirRecursive(contentResolver, uri)
        if (songs.isNotEmpty()) {
            var randomPlaylist: RandomPlaylist? = uri.path?.let {
                playlistsRepo.getPlaylist(it)
            }
            if (randomPlaylist == null) {
                randomPlaylist = uri.path?.let {
                    RandomPlaylist(
                        context,
                        it,
                        songs,
                        false
                    )
                }
                if (randomPlaylist != null) {
                    playlistsRepo.addPlaylist(context, randomPlaylist)
                }
            } else {
                addNewSongs(context, randomPlaylist)
                removeMissingSongs(context, randomPlaylist)
            }
            return randomPlaylist
        }
        return null
    }

    private fun getFilesFromDirRecursive(contentResolver: ContentResolver, rootUri: Uri) {
        val childUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
            rootUri,
            DocumentsContract.getTreeDocumentId(rootUri)
        )
        searchFilesForMusic(contentResolver, childUri, rootUri)
    }

    private fun searchFilesForMusic(contentResolver: ContentResolver, childUri: Uri, rootUri: Uri) {
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
                        searchFilesForMusic(contentResolver, newNode, rootUri)
                    } else {
                        getSong(contentResolver, displayName)?.let { songs.add(it) }
                    }
                }
            }
        }
    }

    private fun getSong(contentResolver: ContentResolver, displayName: String): Song? {
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


    private fun addNewSongs(context: Context, randomPlaylist: RandomPlaylist) {
        for (song in songs) {
            if (!randomPlaylist.contains(song.id)) {
                randomPlaylist.add(
                    context,
                    song
                )
            }
        }
    }

    private fun removeMissingSongs(context: Context, randomPlaylist: RandomPlaylist) {
        for (song in randomPlaylist.getSongs()) {
            if (!songs.contains(song)) {
                randomPlaylist.remove(context, song)
                songs.remove(song)
            }
        }
    }

}
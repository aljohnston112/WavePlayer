package com.fourthFinger.pinkyPlayer.fragments

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.fourthFinger.pinkyPlayer.NavUtil
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song

class ViewModelFragmentTitle : ViewModel() {

    private var songs: MutableList<Song> = mutableListOf()

    fun onViewCreated() {
        songs.clear()
    }

    fun playlistsClicked(navController: NavController) {
        NavUtil.navigate(
            navController,
            FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists()
        )
    }

    fun songsClicked(navController: NavController) {
        NavUtil.navigate(
            navController,
            FragmentTitleDirections.actionFragmentTitleToFragmentSongs()
        )
    }

    fun settingsClicked(navController: NavController) {
        NavUtil.navigate(
            navController,
            FragmentTitleDirections.actionFragmentTitleToFragmentSettings()
        )

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
                        it,
                        songs,
                        false
                    )
                }
                if (randomPlaylist != null) {
                    playlistsRepo.addPlaylist(context, randomPlaylist)
                }
            } else {
                addNewSongs(randomPlaylist)
                removeMissingSongs(randomPlaylist)
            }
            return randomPlaylist
        }
        return null
    }

    private fun getFilesFromDirRecursive(contentResolver: ContentResolver, rootUri: Uri) {
        val childrenUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
            rootUri, DocumentsContract.getTreeDocumentId(rootUri)
        )
        getFiles(contentResolver, childrenUri, rootUri)
    }

    private fun getFiles(contentResolver: ContentResolver, childrenUri: Uri, rootUri: Uri) {
        val selection: String =
            MediaStore.Audio.Media.IS_MUSIC + " != ? OR" +
                    DocumentsContract.Document.COLUMN_MIME_TYPE + " " + "== ?"
        val selectionArgs = arrayOf<String?>("0", DocumentsContract.Document.MIME_TYPE_DIR)
        contentResolver.query(
            childrenUri, arrayOf<String?>(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST_ID,
            ),
            selection, selectionArgs, null
        ).use { cursor ->
            if (cursor != null) {
                val docIdCol: Int = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                )
                val mimeCol: Int = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                val idCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )
                val titleCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )
                val nameCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val docId: String = cursor.getString(docIdCol)
                    val mime: String = cursor.getString(mimeCol)
                    val displayName: String = cursor.getString(nameCol)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val newNode: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                            rootUri, docId
                        )
                        getFiles(contentResolver, newNode, rootUri)
                    } else {
                        val id: Long = cursor.getLong(idCol)
                        val title: String = cursor.getString(titleCol)
                        songs.add(Song(id, title))
                    }
                }
            }
        }
    }

    private fun addNewSongs(randomPlaylist: RandomPlaylist) {
        for (song in songs) {
            if (!randomPlaylist.contains(song)) {
                randomPlaylist.add(song)
            }
        }
    }

    private fun removeMissingSongs(randomPlaylist: RandomPlaylist) {
        for (song in randomPlaylist.getSongs()) {
            if (!songs.contains(song)) {
                randomPlaylist.remove(song)
                songs.remove(song)
            }
        }
    }

}
package com.fourthFinger.pinkyPlayer.fragments

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.FragmentTitleBinding
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.media_controller.SaveFile
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import java.util.*

class FragmentTitle : Fragment() {

    private var _binding: FragmentTitleBinding? = null
    private val binding get() = _binding!!
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()

    private lateinit var mediaData: MediaData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaData = MediaData.getInstance()

    }

    private val onClickListenerFragmentTitleButtons = View.OnClickListener { view: View? ->
        // Add certainButtonClicked methods to a ViewModel
        if (view?.id == R.id.button_playlists) {
            val navController: NavController = NavHostFragment.findNavController(this)
            if (navController.currentDestination?.id == R.id.FragmentTitle) {
                navController.navigate(FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists())
            }
        } else if (view?.id == R.id.button_songs) {
            val navController: NavController = NavHostFragment.findNavController(this)
            if (navController.currentDestination?.id == R.id.FragmentTitle) {
                navController.navigate(FragmentTitleDirections.actionFragmentTitleToFragmentSongs())
            }
        } else if (view?.id == R.id.button_settings) {
            val navController: NavController = NavHostFragment.findNavController(this)
            if (navController.currentDestination?.id == R.id.FragmentTitle) {
                navController.navigate(FragmentTitleDirections.actionFragmentTitleToFragmentSettings())
            }
        } else if (view?.id == R.id.button_folder_search) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val title = view.resources.getString(R.string.pick_folder)
            val chooser: Intent = Intent.createChooser(intent, title)
            startActivityForResult(chooser, REQUEST_CODE_OPEN_FOLDER)
            binding.buttonFolderSearch.setOnClickListener(null)
        }
    }

    private var uriUserPickedFolder: Uri? = null
    private var songs: MutableList<Song> = mutableListOf()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTitleBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        songs.clear()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.app_name))
        viewModelActivityMain.showFab(false)
        setUpButtons()
    }

    private fun setUpButtons() {
        binding.buttonPlaylists.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSongs.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSettings.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonFolderSearch.setOnClickListener(onClickListenerFragmentTitleButtons)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        binding.buttonFolderSearch.setOnClickListener(onClickListenerFragmentTitleButtons)
        if (requestCode == REQUEST_CODE_OPEN_FOLDER && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                val uri: Uri? = resultData.data
                if(uri != null) {
                    uriUserPickedFolder = uri
                    getFilesFromDirRecursive(uri)
                    if (uriUserPickedFolder != null) {
                        if (songs.isNotEmpty()) {
                            var randomPlaylist: RandomPlaylist? = uriUserPickedFolder?.path?.let {
                                viewModelPlaylists.getPlaylist(it)
                            }
                            if (randomPlaylist == null) {
                                randomPlaylist = uriUserPickedFolder?.path?.let {
                                    RandomPlaylist(
                                            it,
                                            songs,
                                            false
                                    )
                                }
                                if (randomPlaylist != null) {
                                    viewModelPlaylists.addNewPlaylist(
                                        requireActivity().applicationContext,
                                        randomPlaylist
                                    )
                                }
                            } else {
                                addNewSongs(randomPlaylist)
                                removeMissingSongs(randomPlaylist)
                            }
                            viewModelPlaylists.setUserPickedPlaylist(randomPlaylist)
                        }
                        SaveFile.saveFile(requireActivity().applicationContext)
                        NavHostFragment.findNavController(this@FragmentTitle)
                                .navigate(FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists())
                    }
                }
            }
        }
    }

    private fun getFilesFromDirRecursive(rootUri: Uri) {
        val childrenUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                rootUri, DocumentsContract.getTreeDocumentId(rootUri))
        getFiles(childrenUri, rootUri)
    }

    private fun getFiles(childrenUri: Uri, rootUri: Uri) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val contentResolver: ContentResolver = activityMain.contentResolver
        val selection: String = MediaStore.Audio.Media.IS_MUSIC + " != ? OR" + DocumentsContract.Document.COLUMN_MIME_TYPE + " == ?"
        val selectionArgs = arrayOf<String?>("0", DocumentsContract.Document.MIME_TYPE_DIR)
        contentResolver.query(childrenUri, arrayOf<String?>(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DATA),
                selection, selectionArgs, null).use { cursor ->
            if (cursor != null) {
                val nameCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val docId: String = cursor.getString(0)
                    val mime: String = cursor.getString(1)
                    val displayName: String = cursor.getString(nameCol)
                    if (DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                        val newNode: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                                rootUri, docId)
                        getFiles(newNode, rootUri)
                    } else {
                        getSong(displayName)?.let { songs.add(it) }
                    }
                }
            }
        }
    }

    private fun getSong(displayName: String?): Song? {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val contentResolver: ContentResolver = activityMain.contentResolver
        val selection: String = MediaStore.Audio.Media.DISPLAY_NAME + " == ?"
        val selectionArgs = arrayOf(displayName)
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, arrayOf<String?>(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE),
                selection, selectionArgs, null).use { cursor ->
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    val idCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val id: Long = cursor.getLong(idCol)
                    val title: String = cursor.getString(titleCol)
                    return Song(id, title)
                }
            }
        }
        return null
    }

    private fun removeMissingSongs(randomPlaylist: RandomPlaylist) {
        for (song in randomPlaylist.getSongs()) {
            if (!songs.contains(song)) {
                randomPlaylist.remove(song)
                songs.remove(song)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_CODE_OPEN_FOLDER = 9367
    }

}
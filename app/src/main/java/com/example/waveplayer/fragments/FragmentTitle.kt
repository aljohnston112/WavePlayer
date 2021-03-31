package com.example.waveplayer.fragments

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.fragment.app.Fragment
import com.example.waveplayer.databinding.FragmentTitleBinding
import java.util.*

class FragmentTitle : Fragment() {
    private var binding: FragmentTitleBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var viewModelUserPickedPlaylist: ViewModelUserPickedPlaylist? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var onClickListenerFragmentTitleButtons: View.OnClickListener? = null
    private var uriUserPickedFolder: Uri? = null
    private var songs: MutableList<Song?>? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModelUserPickedPlaylist = ViewModelProvider(requireActivity()).get<ViewModelUserPickedPlaylist?>(ViewModelUserPickedPlaylist::class.java)
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
        binding = FragmentTitleBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        songs = ArrayList<Song?>()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.app_name))
        viewModelActivityMain.showFab(false)
        setUpButtons()
        setUpBroadCastReceiver()
    }

    private fun setUpButtons() {
        onClickListenerFragmentTitleButtons = View.OnClickListener { view: View? ->
            if (view.getId() == R.id.button_playlists) {
                val navController: NavController = NavHostFragment.findNavController(this)
                if (navController.getCurrentDestination().getId() == R.id.FragmentTitle) {
                    navController.navigate(FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists())
                }
            } else if (view.getId() == R.id.button_songs) {
                val navController: NavController = NavHostFragment.findNavController(this)
                if (navController.getCurrentDestination().getId() == R.id.FragmentTitle) {
                    navController.navigate(FragmentTitleDirections.actionFragmentTitleToFragmentSongs())
                }
            } else if (view.getId() == R.id.button_settings) {
                val navController: NavController = NavHostFragment.findNavController(this)
                if (navController.getCurrentDestination().getId() == R.id.FragmentTitle) {
                    navController.navigate(FragmentTitleDirections.actionFragmentTitleToFragmentSettings())
                }
            } else if (view.getId() == R.id.button_folder_search) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val title = view.getResources().getString(R.string.pick_folder)
                val chooser: Intent = Intent.createChooser(intent, title)
                startActivityForResult(chooser, REQUEST_CODE_OPEN_FOLDER)
                binding.buttonFolderSearch.setOnClickListener(null)
            }
        }
        binding.buttonPlaylists.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSongs.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonSettings.setOnClickListener(onClickListenerFragmentTitleButtons)
        binding.buttonFolderSearch.setOnClickListener(onClickListenerFragmentTitleButtons)
    }

    // TODO possibly delete
    private fun setUpBroadCastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val intentFilterServiceConnected = IntentFilter()
        intentFilterServiceConnected.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilterServiceConnected.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected))
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {}
        }
        activityMain.registerReceiver(broadcastReceiver, intentFilterServiceConnected)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        binding.buttonFolderSearch.setOnClickListener(onClickListenerFragmentTitleButtons)
        if (requestCode == REQUEST_CODE_OPEN_FOLDER && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                val uri: Uri = resultData.getData()
                uriUserPickedFolder = uri
                getFilesFromDirRecursive(uri)
                if (uriUserPickedFolder != null) {
                    if (!songs.isEmpty()) {
                        var randomPlaylist: RandomPlaylist? = activityMain.getPlaylist(uriUserPickedFolder.getPath())
                        if (randomPlaylist == null) {
                            randomPlaylist = RandomPlaylist(
                                    uriUserPickedFolder.getPath(), songs, activityMain.getMaxPercent(),
                                    false)
                            activityMain.addPlaylist(randomPlaylist)
                        } else {
                            addNewSongs(randomPlaylist)
                            removeMissingSongs(randomPlaylist)
                        }
                        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist)
                    }
                    SaveFile.saveFile(requireActivity().applicationContext)
                    NavHostFragment.findNavController(this@FragmentTitle)
                            .navigate(FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists())
                }
            }
        }
    }

    fun getFilesFromDirRecursive(rootUri: Uri?) {
        val childrenUri: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                rootUri, DocumentsContract.getTreeDocumentId(rootUri))
        getFiles(childrenUri, rootUri)
    }

    private fun getFiles(childrenUri: Uri?, rootUri: Uri?) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val contentResolver: ContentResolver = activityMain.getContentResolver()
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
                        songs.add(getSong(displayName))
                    }
                }
            }
        }
    }

    private fun getSong(displayName: String?): Song? {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val contentResolver: ContentResolver = activityMain.getContentResolver()
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

    private fun removeMissingSongs(randomPlaylist: RandomPlaylist?) {
        for (song in randomPlaylist.getSongs()) {
            if (!songs.contains(song)) {
                randomPlaylist.remove(song)
                songs.remove(song)
            }
        }
    }

    private fun addNewSongs(randomPlaylist: RandomPlaylist?) {
        for (song in songs) {
            if (song != null) {
                if (!randomPlaylist.contains(song)) {
                    randomPlaylist.add(song)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        binding.buttonPlaylists.setOnClickListener(null)
        binding.buttonSongs.setOnClickListener(null)
        binding.buttonSettings.setOnClickListener(null)
        binding.buttonFolderSearch.setOnClickListener(null)
        onClickListenerFragmentTitleButtons = null
        uriUserPickedFolder = null
        for (i in songs.indices) {
            songs.set(i, null)
        }
        songs = null
        viewModelUserPickedPlaylist = null
        viewModelActivityMain = null
        binding = null
    }

    companion object {
        const val REQUEST_CODE_OPEN_FOLDER = 9367
    }
}
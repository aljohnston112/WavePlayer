package com.example.pinky_player.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pinky_player.R
import com.example.pinky_player.ViewModelUserPickedPlaylist
import com.example.pinky_player.activity_main.ActivityMain
import com.example.pinky_player.activity_main.DialogFragmentAddToPlaylist
import com.example.pinky_player.activity_main.ViewModelActivityMain
import com.example.pinky_player.databinding.RecyclerViewSongListBinding
import com.example.pinky_player.media_controller.MediaController
import com.example.pinky_player.media_controller.MediaData
import com.example.pinky_player.random_playlist.Song
import com.example.pinky_player.random_playlist.SongQueue
import java.util.*

class FragmentSongs : Fragment(), RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelUserPickedPlaylist by activityViewModels<ViewModelUserPickedPlaylist>()
    private val songQueue = SongQueue.getInstance()

    private lateinit var mediaData: MediaData
    private lateinit var mediaController: MediaController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaController = MediaController.getInstance(requireActivity().applicationContext)
        mediaData = MediaData.getInstance(requireActivity().applicationContext)

    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (action != null) {
                if (action == resources.getString(
                                R.string.broadcast_receiver_action_on_create_options_menu)) {
                    setUpToolbar()
                } else if (action == resources.getString(
                                R.string.broadcast_receiver_action_service_connected)) {
                    setUpRecyclerView()
                }
            }
        }
    }

    private var recyclerViewSongs: RecyclerView? = null
    private var recyclerViewAdapterSongs: RecyclerViewAdapterSongs? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View {
        _binding = RecyclerViewSongListBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.hideKeyboard(view)
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.songs))
        viewModelActivityMain.showFab(false)
        viewModelUserPickedPlaylist.setUserPickedPlaylist(mediaData.getMasterPlaylist())
    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val intentFilter = IntentFilter()
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(activityMain.resources.getString(
                R.string.broadcast_receiver_action_service_connected))
        intentFilter.addAction(activityMain.resources.getString(
                R.string.broadcast_receiver_action_on_create_options_menu))
        activityMain.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun setUpToolbar() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val toolbar: Toolbar = activityMain.findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).isVisible = true
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).isVisible = true
            val itemSearch = menu.findItem(R.id.action_search)
            if (itemSearch != null) {
                val searchView = itemSearch.actionView as SearchView
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        filterSongs(newText)
                        return true
                    }
                })
            }
        }
    }

    private fun filterSongs(newText: String?) {
        val recyclerViewSongs: RecyclerView = binding.recyclerViewSongList
        val recyclerViewAdapterSongs = recyclerViewSongs.adapter as RecyclerViewAdapterSongs
        val songs: List<Song>? = mediaData.getAllSongs()
        val sifted: MutableList<Song> = ArrayList<Song>()
        if(songs != null) {
            if (newText != null && newText != "") {
                for (song in songs) {
                    if (song.title.toLowerCase(Locale.ROOT).contains(newText.toLowerCase(Locale.ROOT))) {
                        sifted.add(song)
                    }
                }
                recyclerViewAdapterSongs.updateList(sifted)
            } else {
                recyclerViewAdapterSongs.updateList(songs)
            }
        }
    }

    private fun setUpRecyclerView() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        recyclerViewSongs = binding.recyclerViewSongList
        val songs: List<Song>? = mediaData.getAllSongs()
        if (songs != null) {
            recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
                    this,
                    ArrayList(songs)
            )
            recyclerViewSongs?.layoutManager = LinearLayoutManager(recyclerViewSongs?.context)
            recyclerViewSongs?.adapter = recyclerViewAdapterSongs
        }
        val searchView: SearchView = activityMain.findViewById<Toolbar>(
            R.id.toolbar
        ).menu?.findItem(R.id.action_search)?.actionView as SearchView
        if(searchView.query.isNotEmpty()){
            filterSongs(searchView.query.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        setUpToolbar()
        setUpRecyclerView()
    }

    override fun onMenuItemClickAddToPlaylist(song: Song): Boolean {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song)
        val dialogFragmentAddToPlaylist = DialogFragmentAddToPlaylist()
        dialogFragmentAddToPlaylist.arguments = bundle
        dialogFragmentAddToPlaylist.show(parentFragmentManager, tag)
        return true
    }

    override fun onMenuItemClickAddToQueue(song: Song): Boolean {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        if (mediaController.isSongInProgress()) {
            songQueue.addToQueue(song.id)
        } else {
            activityMain.showSongPane()
            songQueue.addToQueue(song.id)
            if (!mediaController.isSongInProgress()) {
                mediaController.playNext()
            }
        }
        return true
    }

    override fun onClickViewHolder(song: Song) {
        if (song == mediaController.currentAudioUri.value?.id?.let { mediaData.getSong(it) }) {
            mediaController.seekTo(0)
        }
        mediaController.setCurrentPlaylistToMaster()
        songQueue.clearSongQueue()
        songQueue.addToQueue(song.id)
        mediaController.playNext()
        val action: NavDirections = FragmentSongsDirections.actionFragmentSongsToFragmentSong()
        val navController: NavController = NavHostFragment.findNavController(this)
        if (navController.currentDestination?.id == R.id.fragmentSongs) {
            navController.navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.hideKeyboard(requireView())
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            val itemSearch = menu.findItem(R.id.action_search)
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(null)
            searchView.onActionViewCollapsed()
        }
    }
}
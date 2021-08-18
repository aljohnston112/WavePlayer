package com.fourthFinger.pinkyPlayer.fragments

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
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.ViewModelUserPickedSongs
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.RecyclerViewSongListBinding
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import java.util.*

class FragmentSelectSongs : Fragment(), RecyclerViewAdapterSelectSongs.ListenerCallbackSelectSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelUserPickedSongs by activityViewModels<ViewModelUserPickedSongs>()
    private lateinit var mediaData: MediaData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaData = MediaData.getInstance(requireActivity().applicationContext)

    }

    private val broadcastReceiver = object : BroadcastReceiver() {
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

    private var recyclerViewSongList: RecyclerView? = null
    private var recyclerViewAdapter: RecyclerViewAdapterSelectSongs? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = RecyclerViewSongListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.hideKeyboard(view)
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.select_songs))
        setUpRecyclerView()
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
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).isVisible = true
            val itemSearch = menu.findItem(R.id.action_search)
            if (itemSearch != null) {
                val onQueryTextListenerSearch = object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val songs = mediaData.getAllSongs()
                        if (songs != null) {
                            val sifted = mutableListOf<Song>()
                            if (newText != null && newText != "") {
                                for (song in songs) {
                                    if (song.title.toLowerCase(Locale.ROOT).contains(newText.toLowerCase(Locale.ROOT))) {
                                        sifted.add(song)
                                    }
                                }
                                recyclerViewAdapter?.updateList(sifted)
                            } else {
                                recyclerViewAdapter?.updateList(songs)
                            }
                        }
                        return true
                    }
                }
                val searchView = itemSearch.actionView as SearchView
                searchView.setOnQueryTextListener(onQueryTextListenerSearch)
            }
        }
    }

    private fun setUpRecyclerView() {
        recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList?.layoutManager = LinearLayoutManager(requireContext())
        for (song in viewModelUserPickedSongs.getUserPickedSongs()) {
            song.setSelected(true)
        }
        recyclerViewAdapter = mediaData.getAllSongs()?.let {
            RecyclerViewAdapterSelectSongs(this, it)
        }
        recyclerViewSongList?.adapter = recyclerViewAdapter
    }

    override fun onResume() {
        super.onResume()
        setUpToolbar()
        updateFAB()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_check_white_24dp)
        viewModelActivityMain.setFABText(R.string.fab_done)
        viewModelActivityMain.showFab(true)
        viewModelActivityMain.setFabOnClickListener{
            val navController: NavController = NavHostFragment.findNavController(this)
            if (navController.currentDestination?.id == R.id.FragmentSelectSongs) {
                navController.popBackStack()
            }
        }
    }

    override fun getUserPickedSongs(): List<Song> {
        return viewModelUserPickedSongs.getUserPickedSongs()
    }

    override fun removeUserPickedSong(song: Song) {
        viewModelUserPickedSongs.removeUserPickedSong(song)
    }

    override fun addUserPickedSong(song: Song) {
        viewModelUserPickedSongs.addUserPickedSong(song)
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        viewModelActivityMain.setFabOnClickListener(null)
        val toolbar: Toolbar = activityMain.findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            val itemSearch = menu.findItem(R.id.action_search)
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(null)
            searchView.onActionViewCollapsed()
        }
    }

}
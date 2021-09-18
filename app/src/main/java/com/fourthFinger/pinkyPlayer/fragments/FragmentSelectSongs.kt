package com.fourthFinger.pinkyPlayer.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
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
import com.fourthFinger.pinkyPlayer.KeyboardUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.RecyclerViewSongListBinding
import com.fourthFinger.pinkyPlayer.random_playlist.Song

class FragmentSelectSongs : Fragment(), RecyclerViewAdapterSelectSongs.ListenerCallbackSelectSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()

    private var recyclerViewSongList: RecyclerView? = null
    private lateinit var recyclerViewAdapter: RecyclerViewAdapterSelectSongs

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
        KeyboardUtil.hideKeyboard(view)
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.select_songs))
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList?.layoutManager = LinearLayoutManager(requireContext())
        // TODO respond to LiveData of all songs
        recyclerViewAdapter = viewModelPlaylists.getAllSongs()?.let {
            RecyclerViewAdapterSelectSongs(this, it)
        }?: RecyclerViewAdapterSelectSongs(this, listOf())
        recyclerViewSongList?.adapter = recyclerViewAdapter
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    private fun setUpBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(requireActivity().resources.getString(
                R.string.broadcast_receiver_action_service_connected))
        requireActivity().registerReceiver(broadcastReceiver, intentFilter)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (action != null) {
                if (action == resources.getString(
                        R.string.broadcast_receiver_action_service_connected)) {
                    setUpRecyclerView()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).isVisible = true
        val itemSearch = menu.findItem(R.id.action_search)
        if (itemSearch != null) {
            val onQueryTextListenerSearch = object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    filterSongs(newText)
                    return true
                }
            }
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(onQueryTextListenerSearch)
        }
    }

    private fun filterSongs(newText: String) {
        if (newText.isNotEmpty()) {
            val sifted = viewModelPlaylists.filterAllSongs(newText)
            recyclerViewAdapter.updateList(sifted)
        } else {
            viewModelPlaylists.getUserPickedPlaylist()?.getSongs()?.let {
                recyclerViewAdapter.updateList(it)
            }
        }
    }

    // TODO should be able to remove
    override fun getUserPickedSongs(): List<Song> {
        return viewModelPlaylists.getUserPickedSongs()
    }

    override fun songUnselected(song: Song) {
        viewModelPlaylists.songUnselected(song)
    }

    override fun songSelected(song: Song) {
        viewModelPlaylists.songSelected(song)
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
        viewModelActivityMain.setFabOnClickListener(null)
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
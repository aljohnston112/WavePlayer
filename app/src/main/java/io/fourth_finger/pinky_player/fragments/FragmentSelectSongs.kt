package io.fourth_finger.pinky_player.fragments

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import io.fourth_finger.pinky_player.KeyboardUtil
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.activity_main.ActivityMain
import io.fourth_finger.pinky_player.activity_main.ViewModelActivityMain
import io.fourth_finger.pinky_player.databinding.RecyclerViewSongListBinding

class FragmentSelectSongs : Fragment(), RecyclerViewAdapterSelectSongs.ListenerCallbackSelectSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain> {
        ViewModelActivityMain.Factory
    }

    private val viewModelFragmentSelectSongs by activityViewModels<ViewModelFragmentSelectSongs> {
        ViewModelFragmentSelectSongs.Factory
    }

    private lateinit var recyclerViewAdapter: RecyclerViewAdapterSelectSongs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RecyclerViewSongListBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtil.hideKeyboard(view)
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.select_songs))
        val playlist = viewModelActivityMain.currentContextPlaylist
        viewModelFragmentSelectSongs.selectSongs(playlist)
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        val recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList.layoutManager = LinearLayoutManager(requireContext())
        // TODO respond to LiveData of all songs
        recyclerViewAdapter = RecyclerViewAdapterSelectSongs(
            this,
            viewModelActivityMain.getAllSongs()
        )
        recyclerViewSongList.adapter = recyclerViewAdapter
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    private fun setUpBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(
            requireActivity().resources.getString(
                R.string.action_service_connected
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                broadcastReceiver,
                intentFilter,
                Service.RECEIVER_EXPORTED
            )
        } else {
            requireActivity().registerReceiver(broadcastReceiver, intentFilter)
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (action != null) {
                if (action == resources.getString(
                        R.string.action_service_connected
                    )
                ) {
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
        viewModelActivityMain.setFabOnClickListener {
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
                    siftSongs(newText)
                    return true
                }
            }
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(onQueryTextListenerSearch)
        }
    }

    private fun siftSongs(newText: String) {
        if (newText.isNotEmpty()) {
            val sifted = viewModelActivityMain.siftAllSongs(newText)
            recyclerViewAdapter.updateList(sifted)
        } else {
            viewModelActivityMain.getAllSongs().let {
                recyclerViewAdapter.updateList(it)
            }
        }
    }

    override fun songUnselected(song: io.fourth_finger.playlist_data_source.Song) {
        viewModelFragmentSelectSongs.unselectedSong(song)
    }

    override fun songSelected(song: io.fourth_finger.playlist_data_source.Song) {
        viewModelFragmentSelectSongs.selectSong(song)
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
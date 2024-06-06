package io.fourthFinger.pinkyPlayer.fragments

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
import androidx.recyclerview.widget.LinearLayoutManager
import io.fourthFinger.pinkyPlayer.KeyboardUtil
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import io.fourthFinger.pinkyPlayer.activity_main.DialogFragmentAddToPlaylist
import io.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import io.fourthFinger.pinkyPlayer.databinding.RecyclerViewSongListBinding

class FragmentSongs : Fragment(), RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>{
        ViewModelActivityMain.Factory
    }

    private lateinit var recyclerViewAdapterSongs: RecyclerViewAdapterSongs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.songs))
        viewModelActivityMain.showFab(false)
        setUpRecyclerView()
        setUpSearchView()
    }

    private fun setUpRecyclerView() {
        val recyclerViewSongs = binding.recyclerViewSongList
        recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
            this,
            viewModelActivityMain.getAllSongs()
        )
        recyclerViewSongs.adapter = recyclerViewAdapterSongs
        recyclerViewSongs.layoutManager = LinearLayoutManager(recyclerViewSongs.context)
    }

    private fun setUpSearchView() {
        val searchView: SearchView = requireActivity().findViewById<Toolbar>(
            R.id.toolbar
        ).menu?.findItem(R.id.action_search)?.actionView as SearchView
        if (searchView.query.isNotEmpty()) {
            filterSongs(searchView.query.toString())
        }
    }

    private fun filterSongs(newText: String?) {
        if (newText != null && newText != "") {
            val sifted = viewModelActivityMain.siftAllSongs(newText)
            recyclerViewAdapterSongs.updateList(sifted)
        } else {
            recyclerViewAdapterSongs.updateList(
                viewModelActivityMain.getAllSongs()
            )
        }
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
            requireActivity().registerReceiver(broadcastReceiver, intentFilter, Service.RECEIVER_EXPORTED)
        } else {
            requireActivity().registerReceiver(broadcastReceiver, intentFilter)
        }    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
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

    private fun setUpToolbar() {
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBABILITIES_INDEX).isVisible = true
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

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        setUpToolbar()
    }

    override fun onMenuItemClickAddToPlaylist(song: io.fourthFinger.playlistDataSource.Song) {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song)
        val dialogFragmentAddToPlaylist = DialogFragmentAddToPlaylist()
        dialogFragmentAddToPlaylist.arguments = bundle
        dialogFragmentAddToPlaylist.show(parentFragmentManager, tag)
    }

    override fun onMenuItemClickAddToQueue(song: io.fourthFinger.playlistDataSource.Song) {
        viewModelActivityMain.addToQueue(requireActivity().applicationContext, song)
    }

    override fun onClickViewHolder(pos: Int, song: io.fourthFinger.playlistDataSource.Song) {
        viewModelActivityMain.playlistSongClicked(
            this,
            song
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        KeyboardUtil.hideKeyboard(requireView())
        _binding = null
    }

    override fun onDestroy() {
        // TODO related to bug where text is cleared?
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
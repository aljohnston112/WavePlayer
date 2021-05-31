package com.example.waveplayer2.fragments

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
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.waveplayer2.R
import com.example.waveplayer2.ViewModelUserPickedPlaylist
import com.example.waveplayer2.ViewModelUserPickedSongs
import com.example.waveplayer2.activity_main.ActivityMain
import com.example.waveplayer2.activity_main.DialogFragmentAddToPlaylist
import com.example.waveplayer2.activity_main.ViewModelActivityMain
import com.example.waveplayer2.databinding.RecyclerViewPlaylistListBinding
import com.example.waveplayer2.media_controller.MediaController
import com.example.waveplayer2.media_controller.MediaData
import com.example.waveplayer2.random_playlist.RandomPlaylist
import com.example.waveplayer2.random_playlist.SongQueue
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.util.*

class FragmentPlaylists : Fragment(), RecyclerViewAdapterPlaylists.ListenerCallbackPlaylists {

    private var _binding: RecyclerViewPlaylistListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelUserPickedPlaylist by activityViewModels<ViewModelUserPickedPlaylist>()
    private val viewModelUserPickedSongs by activityViewModels<ViewModelUserPickedSongs>()
    private val songQueue = SongQueue.getInstance()

    private lateinit var mediaData: MediaData
    private lateinit var mediaController: MediaController

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
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

    private var recyclerViewPlaylists: RecyclerView? = null
    private var recyclerViewAdapterPlaylists: RecyclerViewAdapterPlaylists? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaController = MediaController.getInstance(requireActivity().applicationContext)
        mediaData = MediaData.getInstance(requireActivity().applicationContext)

    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = RecyclerViewPlaylistListBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.hideKeyboard(requireView())
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.playlists))
        setUpBroadcastReceiver()
        setUpRecyclerView()
    }

    private fun setUpBroadcastReceiver() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val intentFilter = IntentFilter()
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        intentFilter.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected))
        intentFilter.addAction(activityMain.getResources().getString(
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
                val searchView = itemSearch.actionView as SearchView
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        // TODO fix bug where you can reorder songs when sifted
                        recyclerViewPlaylists = binding.recyclerViewPlaylistList
                        recyclerViewAdapterPlaylists = recyclerViewPlaylists?.adapter as RecyclerViewAdapterPlaylists
                        val playlists: List<RandomPlaylist> = mediaData.getPlaylists()
                        val sifted: MutableList<RandomPlaylist> = ArrayList<RandomPlaylist>()
                        if (newText != null && newText != "") {
                            for (randomPlaylist in playlists) {
                                if (randomPlaylist.getName().toLowerCase(Locale.ROOT).contains(newText.toLowerCase(Locale.ROOT))) {
                                    sifted.add(randomPlaylist)
                                }
                            }
                            recyclerViewAdapterPlaylists?.updateList(sifted)
                        } else {
                            recyclerViewAdapterPlaylists?.updateList(playlists)
                        }
                        return true
                    }
                })
            }
        }
    }

    private fun setUpRecyclerView() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val recyclerView: RecyclerView = binding.recyclerViewPlaylistList
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        val recyclerViewAdapter = RecyclerViewAdapterPlaylists(this, mediaData.getPlaylists())
        recyclerView.adapter = recyclerViewAdapter
        ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                Collections.swap(
                        mediaData.getPlaylists(),
                        viewHolder.adapterPosition,
                        target.adapterPosition
                )
                recyclerViewAdapter.notifyItemMoved(
                        viewHolder.adapterPosition,
                        target.adapterPosition
                )
                activityMain.saveFile()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position: Int = viewHolder.adapterPosition
                val randomPlaylists: List<RandomPlaylist> = mediaData.getPlaylists()
                val randomPlaylist: RandomPlaylist = randomPlaylists[position]
                mediaData.removePlaylist(randomPlaylist)
                recyclerViewAdapter.notifyItemRemoved(position)
                activityMain.saveFile()
                val snackbar: Snackbar = Snackbar.make(binding.recyclerViewPlaylistList,
                        R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG)
                snackbar.setAction(R.string.undo) {
                    mediaData.addPlaylist(position, randomPlaylist)
                    recyclerViewAdapter.notifyItemInserted(position)
                    activityMain.saveFile()
                }
                snackbar.show()
            }
        }).attachToRecyclerView(recyclerView)
    }

    override fun onResume() {
        super.onResume()
        updateFAB()
        setUpToolbar()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_new)
        viewModelActivityMain.showFab(true)
        viewModelActivityMain.setFabOnClickListener {
            // userPickedPlaylist is null when user is making a new playlist
            viewModelUserPickedPlaylist.setUserPickedPlaylist(null)
            viewModelUserPickedSongs.clearUserPickedSongs()
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist())
        }
    }

    override fun onMenuItemClickAddToPlaylist(randomPlaylist: RandomPlaylist): Boolean {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, randomPlaylist)
        val dialogFragmentAddToPlaylist = DialogFragmentAddToPlaylist()
        dialogFragmentAddToPlaylist.arguments = bundle
        dialogFragmentAddToPlaylist.show(parentFragmentManager, tag)
        return true
    }

    override fun onMenuItemClickAddToQueue(randomPlaylist: RandomPlaylist): Boolean {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val songs = randomPlaylist.getSongs()
        for (song in songs) {
            songQueue.addToQueue(song.id)
        }
        // TODO stop MasterPlaylist from continuing after queue is done
        // shuffle is off and looping is on or something like that?
        mediaController.setCurrentPlaylistToMaster()
        if (!mediaController.isSongInProgress()) {
            activityMain.showSongPane()
            // TODO goToFrontOfQueue() is dumb
            songQueue.goToFront()
            mediaController.playNext()
        }
        return true
    }

    override fun onClickViewHolder(randomPlaylist: RandomPlaylist) {
        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist)
        NavHostFragment.findNavController(this).navigate(
                FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        super.onDestroy()
        activityMain.unregisterReceiver(broadcastReceiver)
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
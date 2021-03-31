package com.example.waveplayer.fragments

import android.content.Context
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.waveplayer.databinding.RecyclerViewPlaylistListBinding
import java.util.*

class FragmentPlaylists : Fragment(), ListenerCallbackPlaylists {
    private var binding: RecyclerViewPlaylistListBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var viewModelUserPickedPlaylist: ViewModelUserPickedPlaylist? = null
    private var viewModelUserPickedSongs: ViewModelUserPickedSongs? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var recyclerViewPlaylists: RecyclerView? = null
    private var recyclerViewAdapterPlaylists: RecyclerViewAdapterPlaylists? = null
    private var onClickListenerFAB: View.OnClickListener? = null
    private var onQueryTextListenerSearch: SearchView.OnQueryTextListener? = null
    private var itemTouchHelper: ItemTouchHelper? = null
    private var itemTouchHelperCallback: ItemTouchHelper.Callback? = null
    private var undoListenerPlaylistRemoved: View.OnClickListener? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setUpViewModels()
        binding = RecyclerViewPlaylistListBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    private fun setUpViewModels() {
        viewModelUserPickedPlaylist = ViewModelProvider(requireActivity()).get<ViewModelUserPickedPlaylist?>(ViewModelUserPickedPlaylist::class.java)
        viewModelUserPickedSongs = ViewModelProvider(requireActivity()).get<ViewModelUserPickedSongs?>(ViewModelUserPickedSongs::class.java)
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
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
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action: String = intent.getAction()
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
        activityMain.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun setUpToolbar() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val toolbar: Toolbar = activityMain.findViewById<Toolbar?>(R.id.toolbar)
        if (toolbar != null) {
            val menu = toolbar.menu
            if (menu != null) {
                menu.getItem(ActivityMain.Companion.MENU_ACTION_SEARCH_INDEX).isVisible = true
                val itemSearch = menu.findItem(R.id.action_search)
                if (itemSearch != null) {
                    onQueryTextListenerSearch = object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            // TODO fix bug where you can reorder songs when sifted
                            recyclerViewPlaylists = binding.recyclerViewPlaylistList
                            recyclerViewAdapterPlaylists = recyclerViewPlaylists.getAdapter() as RecyclerViewAdapterPlaylists
                            val playlists: MutableList<RandomPlaylist?> = activityMain.getPlaylists()
                            val sifted: MutableList<RandomPlaylist?> = ArrayList<RandomPlaylist?>()
                            if (newText != "") {
                                for (randomPlaylist in playlists) {
                                    if (randomPlaylist.getName().toLowerCase().contains(newText.toLowerCase())) {
                                        sifted.add(randomPlaylist)
                                    }
                                }
                                recyclerViewAdapterPlaylists.updateList(sifted)
                            } else {
                                recyclerViewAdapterPlaylists.updateList(playlists)
                            }
                            return true
                        }
                    }
                    val searchView = itemSearch.actionView as SearchView
                    searchView.setOnQueryTextListener(onQueryTextListenerSearch)
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val recyclerView: RecyclerView = binding.recyclerViewPlaylistList
        recyclerView.setLayoutManager(LinearLayoutManager(recyclerView.getContext()))
        val recyclerViewAdapter = RecyclerViewAdapterPlaylists(this, activityMain.getPlaylists())
        recyclerView.setAdapter(recyclerViewAdapter)
        itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                Collections.swap(activityMain.getPlaylists(),
                        viewHolder.getAdapterPosition(), target.getAdapterPosition())
                recyclerViewAdapter.notifyItemMoved(
                        viewHolder.getAdapterPosition(), target.getAdapterPosition())
                activityMain.saveFile()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position: Int = viewHolder.getAdapterPosition()
                val randomPlaylists: MutableList<RandomPlaylist?> = activityMain.getPlaylists()
                val randomPlaylist: RandomPlaylist? = randomPlaylists[position]
                activityMain.removePlaylist(randomPlaylist)
                recyclerViewAdapter.notifyItemRemoved(position)
                activityMain.saveFile()
                val snackbar: Snackbar = Snackbar.make(binding.recyclerViewPlaylistList,
                        R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG)
                undoListenerPlaylistRemoved = View.OnClickListener { v: View? ->
                    activityMain.addPlaylist(position, randomPlaylist)
                    recyclerViewAdapter.notifyItemInserted(position)
                    activityMain.saveFile()
                }
                snackbar.setAction(R.string.undo, undoListenerPlaylistRemoved)
                snackbar.show()
            }
        }
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
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
        onClickListenerFAB = View.OnClickListener { view: View? ->
            // userPickedPlaylist is null when user is making a new playlist
            viewModelUserPickedPlaylist.setUserPickedPlaylist(null)
            viewModelUserPickedSongs.clearUserPickedSongs()
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist())
        } as View.OnClickListener
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB)
    }

    override fun onMenuItemClickAddToPlaylist(randomPlaylist: RandomPlaylist?): Boolean {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.Companion.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, randomPlaylist)
        val dialogFragmentAddToPlaylist = DialogFragmentAddToPlaylist()
        dialogFragmentAddToPlaylist.setArguments(bundle)
        dialogFragmentAddToPlaylist.show(parentFragmentManager, tag)
        return true
    }

    override fun onMenuItemClickAddToQueue(randomPlaylist: RandomPlaylist?): Boolean {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        for (song in randomPlaylist.getSongs()) {
            activityMain.addToQueue(song.id)
        }
        // TODO stop MasterPlaylist from continuing after queue is done
        // shuffle is off and looping is on or something like that?
        activityMain.setCurrentPlaylistToMaster()
        if (!activityMain.isSongInProgress()) {
            activityMain.showSongPane()
            // TODO goToFrontOfQueue() is dumb
            activityMain.goToFrontOfQueue()
            activityMain.playNext()
        }
        return true
    }

    override fun onClickViewHolder(randomPlaylist: RandomPlaylist?) {
        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist)
        NavHostFragment.findNavController(this).navigate(
                FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentPlaylist())
    }

    override fun onDestroy() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        super.onDestroy()
        activityMain.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        itemTouchHelper.attachToRecyclerView(null)
        itemTouchHelperCallback = null
        itemTouchHelper = null
        undoListenerPlaylistRemoved = null
        val toolbar: Toolbar = activityMain.findViewById<Toolbar?>(R.id.toolbar)
        if (toolbar != null) {
            val menu = toolbar.menu
            if (menu != null) {
                val itemSearch = menu.findItem(R.id.action_search)
                val searchView = itemSearch.actionView as SearchView
                searchView.setOnQueryTextListener(null)
                searchView.onActionViewCollapsed()
            }
        }
        onQueryTextListenerSearch = null
        viewModelActivityMain.setFabOnClickListener(null)
        onClickListenerFAB = null
        if (recyclerViewPlaylists != null) {
            recyclerViewPlaylists.setAdapter(null)
        }
        recyclerViewAdapterPlaylists = null
        recyclerViewPlaylists = null
        viewModelUserPickedPlaylist = null
        viewModelUserPickedSongs = null
        viewModelActivityMain = null
        binding = null
    }
}
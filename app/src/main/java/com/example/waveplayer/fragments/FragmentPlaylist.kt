package com.example.waveplayer.fragments

import android.content.Context
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.waveplayer.databinding.RecyclerViewSongListBinding
import java.util.*

class FragmentPlaylist : Fragment(), ListenerCallbackSongs {
    private var binding: RecyclerViewSongListBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var viewModelUserPickedPlaylist: ViewModelUserPickedPlaylist? = null
    private var viewModelUserPickedSongs: ViewModelUserPickedSongs? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var recyclerViewSongList: RecyclerView? = null
    private var recyclerViewAdapterSongs: RecyclerViewAdapterSongs? = null
    private var onClickListenerFAB: View.OnClickListener? = null
    private var onQueryTextListener: SearchView.OnQueryTextListener? = null
    private var itemTouchHelperCallback: ItemTouchHelper.Callback? = null
    private var itemTouchHelper: ItemTouchHelper? = null
    private var undoListenerSongRemoved: View.OnClickListener? = null
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        createViewModels()
        binding = RecyclerViewSongListBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    private fun createViewModels() {
        viewModelUserPickedPlaylist = ViewModelProvider(requireActivity()).get<ViewModelUserPickedPlaylist?>(ViewModelUserPickedPlaylist::class.java)
        viewModelUserPickedSongs = ViewModelProvider(requireActivity()).get<ViewModelUserPickedSongs?>(ViewModelUserPickedSongs::class.java)
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.hideKeyboard(view)
        val randomPlaylist: RandomPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist()
        viewModelActivityMain.setPlaylistToAddToQueue(randomPlaylist)
        viewModelActivityMain.setActionBarTitle(randomPlaylist.getName())
        setUpBroadcastReceiver(randomPlaylist)
        setUpRecyclerView(randomPlaylist)
    }

    private fun setUpBroadcastReceiver(randomPlaylist: RandomPlaylist?) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu))
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected))
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action: String = intent.getAction()
                if (action != null) {
                    if (action == resources.getString(
                                    R.string.broadcast_receiver_action_on_create_options_menu)) {
                        setUpToolbar()
                    } else if (action == resources.getString(
                                    R.string.broadcast_receiver_action_service_connected)) {
                        setUpRecyclerView(randomPlaylist)
                    }
                }
            }
        }
        activityMain.registerReceiver(broadcastReceiver, filterComplete)
    }

    private fun setUpToolbar() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val toolbar: Toolbar = activityMain.findViewById<Toolbar?>(R.id.toolbar)
        if (toolbar != null) {
            val menu = toolbar.menu
            if (menu != null) {
                menu.getItem(ActivityMain.Companion.MENU_ACTION_RESET_PROBS_INDEX).isVisible = true
                menu.getItem(ActivityMain.Companion.MENU_ACTION_LOWER_PROBS_INDEX).isVisible = true
                menu.getItem(ActivityMain.Companion.MENU_ACTION_ADD_TO_QUEUE).isVisible = true
                menu.getItem(ActivityMain.Companion.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).isVisible = true
                menu.getItem(ActivityMain.Companion.MENU_ACTION_SEARCH_INDEX).isVisible = true
                val itemSearch = menu.findItem(R.id.action_search)
                if (itemSearch != null) {
                    onQueryTextListener = object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            // TODO fix bug where you can reorder songs when sifted
                            val songs: MutableList<Song?> = activityMain.getUserPickedPlaylist().getSongs()
                            val sifted: MutableList<Song?> = ArrayList<Song?>()
                            if (newText != "") {
                                for (song in songs) {
                                    if (song.title.toLowerCase().contains(newText.toLowerCase())) {
                                        sifted.add(song)
                                    }
                                }
                                recyclerViewAdapterSongs.updateList(sifted)
                            } else {
                                recyclerViewAdapterSongs.updateList(songs)
                            }
                            return true
                        }
                    }
                    val searchView = itemSearch.actionView as SearchView
                    searchView.setOnQueryTextListener(null)
                    searchView.setOnQueryTextListener(onQueryTextListener)
                }
            }
        }
    }

    private fun setUpRecyclerView(userPickedPlaylist: RandomPlaylist?) {
        recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList.setLayoutManager(LinearLayoutManager(recyclerViewSongList.getContext()))
        recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
                this,
                userPickedPlaylist.getSongs())
        recyclerViewSongList.setAdapter(recyclerViewAdapterSongs)
        itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView,
                                          viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                val activityMain: ActivityMain = requireActivity() as ActivityMain
                // TODO hopefully not needed
                /*
                    Collections.swap(recyclerViewAdapterSongs.getSongs(),
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());

                 */userPickedPlaylist.swapSongPositions(
                        viewHolder.getAdapterPosition(), target.getAdapterPosition())
                recyclerViewAdapterSongs.notifyItemMoved(
                        viewHolder.getAdapterPosition(), target.getAdapterPosition())
                activityMain.saveFile()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val activityMain: ActivityMain = requireActivity() as ActivityMain
                val position: Int = viewHolder.getAdapterPosition()
                val song: Song = userPickedPlaylist.getSongs().get(position)
                val probability: Double = userPickedPlaylist.getProbability(song)
                if (userPickedPlaylist.size() == 1) {
                    activityMain.removePlaylist(userPickedPlaylist)
                    viewModelUserPickedPlaylist.setUserPickedPlaylist(null)
                } else {
                    userPickedPlaylist.remove(song)
                }
                // TODO needed?
                /*
                recyclerViewAdapterSongs.getSongs().remove(position);
                 */recyclerViewAdapterSongs.notifyItemRemoved(position)
                activityMain.saveFile()
                undoListenerSongRemoved = View.OnClickListener { v: View? ->
                    userPickedPlaylist.add(song, probability)
                    userPickedPlaylist.switchSongPositions(userPickedPlaylist.size() - 1, position)
                    // TODO needed?
                    /*
                    recyclerViewAdapterSongs.updateList(userPickedPlaylist.getSongs());
                 */recyclerViewAdapterSongs.notifyDataSetChanged()
                    activityMain.saveFile()
                }
                val snackbar: Snackbar = Snackbar.make(
                        binding.recyclerViewSongList,
                        R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG)
                snackbar.setAction(R.string.undo, undoListenerSongRemoved)
                snackbar.show()
            }
        }
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewSongList)
    }

    override fun onResume() {
        super.onResume()
        updateFAB()
        setUpToolbar()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_edit)
        viewModelActivityMain.showFab(true)
        viewModelActivityMain.setFabOnClickListener(null)
        onClickListenerFAB = View.OnClickListener { view: View? ->
            // userPickedSongs.isEmpty() when the user is editing a playlist
            viewModelUserPickedSongs.clearUserPickedSongs()
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist())
        }
        viewModelActivityMain.setFabOnClickListener(onClickListenerFAB)
    }

    override fun onMenuItemClickAddToPlaylist(song: Song?): Boolean {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.Companion.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song)
        val dialogFragment: DialogFragment = DialogFragmentAddToPlaylist()
        dialogFragment.arguments = bundle
        dialogFragment.show(parentFragmentManager, tag)
        return true
    }

    override fun onMenuItemClickAddToQueue(song: Song?): Boolean {
        // TODO fix how music continues after queue is depleted
        // shuffle is off and looping is on or something like that?
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.addToQueue(song.id)
        if (!activityMain.isSongInProgress()) {
            activityMain.showSongPane()
            activityMain.playNext()
        }
        return true
    }

    override fun onClickViewHolder(song: Song?) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        synchronized(ActivityMain.Companion.MUSIC_CONTROL_LOCK) {
            if (activityMain.getCurrentAudioUri() != null && song == activityMain.getSong(
                            activityMain.getCurrentAudioUri().id)) {
                activityMain.seekTo(0)
            }
            activityMain.setCurrentPlaylist(viewModelUserPickedPlaylist.getUserPickedPlaylist())
            activityMain.clearSongQueue()
            activityMain.addToQueue(song.id)
            activityMain.playNext()
        }
        val action: NavDirections = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong()
        NavHostFragment.findNavController(this).navigate(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        itemTouchHelper.attachToRecyclerView(null)
        itemTouchHelperCallback = null
        itemTouchHelper = null
        undoListenerSongRemoved = null
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
        onQueryTextListener = null
        viewModelActivityMain.setFabOnClickListener(null)
        onClickListenerFAB = null
        recyclerViewSongList.setAdapter(null)
        recyclerViewAdapterSongs = null
        recyclerViewSongList = null
        viewModelActivityMain.setPlaylistToAddToQueue(null)
        viewModelUserPickedPlaylist = null
        viewModelUserPickedSongs = null
        viewModelActivityMain = null
        binding = null
    }
}
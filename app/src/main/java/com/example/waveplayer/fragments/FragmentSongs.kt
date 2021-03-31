package com.example.waveplayer.fragments

import android.content.Context
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.waveplayer.databinding.RecyclerViewSongListBinding
import java.util.*

class FragmentSongs : Fragment(), ListenerCallbackSongs {
    private var binding: RecyclerViewSongListBinding? = null
    private var viewModelActivityMain: ViewModelActivityMain? = null
    private var viewModelUserPickedPlaylist: ViewModelUserPickedPlaylist? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var recyclerViewSongs: RecyclerView? = null
    private var recyclerViewAdapterSongs: RecyclerViewAdapterSongs? = null
    private var onQueryTextListenerSearch: SearchView.OnQueryTextListener? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewModelUserPickedPlaylist = ViewModelProvider(requireActivity()).get<ViewModelUserPickedPlaylist?>(ViewModelUserPickedPlaylist::class.java)
        viewModelActivityMain = ViewModelProvider(requireActivity()).get<ViewModelActivityMain?>(ViewModelActivityMain::class.java)
        binding = RecyclerViewSongListBinding.inflate(inflater, container, false)
        return binding.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.hideKeyboard(view)
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.songs))
        viewModelActivityMain.showFab(false)
        viewModelUserPickedPlaylist.setUserPickedPlaylist(activityMain.getMasterPlaylist())
        setUpBroadcastReceiver()
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
        val menu = toolbar.menu
        if (menu != null) {
            menu.getItem(ActivityMain.Companion.MENU_ACTION_RESET_PROBS_INDEX).isVisible = true
            menu.getItem(ActivityMain.Companion.MENU_ACTION_SEARCH_INDEX).isVisible = true
            val itemSearch = menu.findItem(R.id.action_search)
            if (itemSearch != null) {
                onQueryTextListenerSearch = object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        val recyclerViewSongs: RecyclerView = activityMain.findViewById<RecyclerView?>(R.id.recycler_view_song_list)
                        if (recyclerViewSongs != null) {
                            val recyclerViewAdapterSongs = recyclerViewSongs.getAdapter() as RecyclerViewAdapterSongs
                            val songs: MutableList<Song?> = activityMain.getAllSongs()
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
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        recyclerViewSongs = binding.recyclerViewSongList
        val songs: MutableList<Song?> = activityMain.getAllSongs()
        if (songs != null) {
            recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
                    this, ArrayList<Song?>(songs))
            recyclerViewSongs.setLayoutManager(LinearLayoutManager(recyclerViewSongs.getContext()))
            recyclerViewSongs.setAdapter(recyclerViewAdapterSongs)
        }
    }

    override fun onResume() {
        super.onResume()
        setUpToolbar()
        setUpRecyclerView()
    }

    override fun onMenuItemClickAddToPlaylist(song: Song?): Boolean {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.Companion.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song)
        val dialogFragmentAddToPlaylist = DialogFragmentAddToPlaylist()
        dialogFragmentAddToPlaylist.setArguments(bundle)
        dialogFragmentAddToPlaylist.show(parentFragmentManager, tag)
        return true
    }

    override fun onMenuItemClickAddToQueue(song: Song?): Boolean {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        if (activityMain.songInProgress()) {
            activityMain.addToQueue(song.id)
        } else {
            activityMain.showSongPane()
            activityMain.addToQueue(song.id)
            if (!activityMain.isSongInProgress()) {
                activityMain.playNext()
            }
        }
        return true
    }

    override fun onClickViewHolder(song: Song?) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        if (song == activityMain.getCurrentSong()) {
            activityMain.seekTo(0)
        }
        activityMain.setCurrentPlaylistToMaster()
        activityMain.clearSongQueue()
        activityMain.addToQueue(song.id)
        activityMain.playNext()
        val action: NavDirections = FragmentSongsDirections.actionFragmentSongsToFragmentSong()
        val navController: NavController = NavHostFragment.findNavController(this)
        if (navController.getCurrentDestination().getId() == R.id.fragmentSongs) {
            navController.navigate(action)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        activityMain.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
        val view = requireView()
        activityMain.hideKeyboard(view)
        val toolbar: Toolbar = activityMain.findViewById<Toolbar?>(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            val itemSearch = menu.findItem(R.id.action_search)
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(null)
            searchView.onActionViewCollapsed()
        }
        onQueryTextListenerSearch = null
        recyclerViewSongs.setAdapter(null)
        recyclerViewAdapterSongs = null
        recyclerViewSongs = null
        viewModelUserPickedPlaylist = null
        viewModelActivityMain = null
        binding = null
    }
}
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import io.fourthFinger.pinkyPlayer.KeyboardUtil
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import io.fourthFinger.pinkyPlayer.activity_main.DialogFragmentAddToPlaylist
import io.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import io.fourthFinger.pinkyPlayer.databinding.RecyclerViewSongListBinding
import io.fourthFinger.playlistDataSource.Song
import java.util.Locale

class FragmentPlaylist : Fragment(), RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain> {
        ViewModelActivityMain.Factory
    }

    private val viewModelPlaylist by activityViewModels<ViewModelFragmentPlaylist> {
        ViewModelFragmentPlaylist.Factory
    }

    private var broadcastReceiver: BroadcastReceiver? = null

    private lateinit var recyclerViewAdapterSongs: RecyclerViewAdapterSongs
    private var dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    private val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

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
        val currentPlaylist = viewModelActivityMain.currentlyPlayingPlaylist.value!!
        viewModelActivityMain.setPlaylistToAddToQueue(currentPlaylist)
        viewModelActivityMain.setActionBarTitle(currentPlaylist.name)
        // TODO why is this set up twice in all the fragments?!
        // I think due to incomplete state
        setUpRecyclerView(currentPlaylist)
        setUpSearchView()
    }

    private fun setUpRecyclerView(userPickedPlaylist: io.fourthFinger.playlistDataSource.RandomPlaylist) {
        val recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList?.layoutManager = LinearLayoutManager(recyclerViewSongList?.context)
        recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
            this,
            userPickedPlaylist.getSongs().toList()
        )
        recyclerViewSongList?.adapter = recyclerViewAdapterSongs
        val itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                viewModelActivityMain.notifySongMoved(
                    requireActivity().applicationContext,
                    viewHolder.absoluteAdapterPosition,
                    target.absoluteAdapterPosition
                )
                recyclerViewAdapterSongs.notifyItemMoved(
                    viewHolder.absoluteAdapterPosition,
                    target.absoluteAdapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                viewModelActivityMain.notifySongRemoved(this@FragmentPlaylist, position)
                recyclerViewAdapterSongs.updateList(userPickedPlaylist.getSongs().toList())
                val snackBar: Snackbar = Snackbar.make(
                    binding.recyclerViewSongList,
                    R.string.song_removed,
                    BaseTransientBottomBar.LENGTH_LONG
                )
                snackBar.setAction(R.string.undo) {
                    viewModelActivityMain.notifyItemInserted(
                        requireActivity().applicationContext,
                        position
                    )
                    recyclerViewAdapterSongs.updateList(userPickedPlaylist.getSongs().toList())
                }
                snackBar.show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerViewSongList)
    }

    private fun setUpSearchView() {
        val searchView: SearchView = requireActivity().findViewById<Toolbar>(
            R.id.toolbar
        ).menu?.findItem(R.id.action_search)?.actionView as SearchView
        if (searchView.query.isNotEmpty()) {
            val newText = searchView.query.toString()
            filterSongs(newText)
        }
    }

    private fun filterSongs(newText: String) {
        dragFlags = if (newText.isNotEmpty()) {
            val sifted = siftPlaylistSongs(newText)
            recyclerViewAdapterSongs.updateList(sifted)
            0
        } else {
            viewModelActivityMain.currentlyPlayingPlaylist.value?.getSongs()?.let {
                recyclerViewAdapterSongs.updateList(it.toList())
            }
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
    }

    // TODO move to view model?
    private fun siftPlaylistSongs(string: String): List<Song> {
        val songs: Set<Song>? = viewModelActivityMain.currentlyPlayingPlaylist.value?.getSongs()
        val sifted: MutableList<Song> = ArrayList<Song>()
        if (songs != null) {
            for (song in songs) {
                if (song.title.lowercase(Locale.ROOT)
                        .contains(string.lowercase(Locale.ROOT))
                ) {
                    sifted.add(song)
                }
            }
        }
        return sifted
    }

    override fun onStart() {
        super.onStart()
        val randomPlaylist: io.fourthFinger.playlistDataSource.RandomPlaylist? = viewModelActivityMain.currentlyPlayingPlaylist.value
        if (randomPlaylist != null) {
            setUpBroadcastReceiver(randomPlaylist)
        }
    }

    private fun setUpBroadcastReceiver(randomPlaylist: io.fourthFinger.playlistDataSource.RandomPlaylist) {
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(
            requireActivity().resources.getString(
                R.string.action_service_connected
            )
        )
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action: String? = intent.action
                if (action != null) {
                    if (action == resources.getString(
                            R.string.action_service_connected
                        )
                    ) {
                        setUpRecyclerView(randomPlaylist)
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                broadcastReceiver,
                filterComplete,
                Service.RECEIVER_EXPORTED
            )
        } else {
            requireActivity().registerReceiver(broadcastReceiver, filterComplete)
        }
    }

    override fun onResume() {
        super.onResume()
        updateFAB()
    }

    private fun updateFAB() {
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_edit)
        viewModelActivityMain.showFab(true)
        viewModelActivityMain.setFabOnClickListener {
            viewModelPlaylist.fabClicked(
                NavHostFragment.findNavController(this)
            )
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBABILITIES_INDEX).isVisible = true
        menu.getItem(ActivityMain.MENU_ACTION_LOWER_PROBABILITIES_INDEX).isVisible = true
        menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE_INDEX).isVisible = true
        menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).isVisible = true
        menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).isVisible = true
        val itemSearch = menu.findItem(R.id.action_search)
        if (itemSearch != null) {
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(onQueryTextListener)
        }
    }

    private val onQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String): Boolean {
            filterSongs(newText)
            return true
        }
    }

    override fun onMenuItemClickAddToPlaylist(song: Song) {
        val bundle = Bundle()
        bundle.putSerializable(
            DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG,
            song
        )
        val dialogFragment: DialogFragment = DialogFragmentAddToPlaylist()
        dialogFragment.arguments = bundle
        dialogFragment.show(parentFragmentManager, tag)
    }

    override fun onMenuItemClickAddToQueue(song: Song) {
        viewModelActivityMain.addToQueue(
            requireActivity().applicationContext,
            song
        )
    }

    override fun onClickViewHolder(pos: Int, song: Song) {
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
        _binding = null
        viewModelActivityMain.setPlaylistToAddToQueue(null)
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            val itemSearch = menu.findItem(R.id.action_search)
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(null)
            searchView.onActionViewCollapsed()
        }
        viewModelActivityMain.setFabOnClickListener(null)
    }

}
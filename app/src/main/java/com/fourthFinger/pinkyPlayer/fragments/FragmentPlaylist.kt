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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fourthFinger.pinkyPlayer.KeyboardUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import com.fourthFinger.pinkyPlayer.activity_main.DialogFragmentAddToPlaylist
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.RecyclerViewSongListBinding
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class FragmentPlaylist : Fragment(), RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelUserPicks by activityViewModels<ViewModelUserPicks>()
    private val viewModelAddToQueue by activityViewModels<ViewModelAddToQueue>()

    private var broadcastReceiver: BroadcastReceiver? = null

    private var recyclerViewSongList: RecyclerView? = null
    private lateinit var recyclerViewAdapterSongs: RecyclerViewAdapterSongs

    var dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

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
        val randomPlaylist: RandomPlaylist? = viewModelUserPicks.getUserPickedPlaylist()
        if (randomPlaylist != null) {
            viewModelAddToQueue.setPlaylistToAddToQueue(randomPlaylist)
            viewModelActivityMain.setActionBarTitle(randomPlaylist.getName())
            // TODO why is this set up twice in all the fragments?!
            // I think due to incomplete state
            setUpRecyclerView(randomPlaylist)
            setUpSearchView()
        }
    }

    private fun setUpRecyclerView(userPickedPlaylist: RandomPlaylist) {
        recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList?.layoutManager = LinearLayoutManager(recyclerViewSongList?.context)
        recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
            this,
            userPickedPlaylist.getSongs()
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
                // TODO hopefully not needed
                /*
                    Collections.swap(recyclerViewAdapterSongs.getSongs(),
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());

                 */
                viewModelUserPicks.notifySongMoved(
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
                // TODO there might be a bug where the last song shows up once for every time a song is swiped.
                val position = viewHolder.absoluteAdapterPosition
                viewModelUserPicks.notifySongRemoved(requireActivity().applicationContext, position)
                // TODO needed?
                /*
                recyclerViewAdapterSongs.getSongs().remove(position);
                 */
                recyclerViewAdapterSongs.notifyItemRemoved(position)
                val snackBar: Snackbar = Snackbar.make(
                    binding.recyclerViewSongList,
                    R.string.song_removed,
                    BaseTransientBottomBar.LENGTH_LONG
                )
                snackBar.setAction(R.string.undo) {
                    viewModelUserPicks.notifyItemInserted(requireActivity().applicationContext, position)
                    // TODO needed?
                    /*
                    recyclerViewAdapterSongs.updateList(userPickedPlaylist.getSongs());
                 */
                    // TODO make sure position is right
                    recyclerViewAdapterSongs.notifyItemInserted(position)
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
            val sifted = viewModelUserPicks.filterPlaylistSongs(newText)
            recyclerViewAdapterSongs.updateList(sifted)
            0
        } else {
            viewModelUserPicks.getUserPickedPlaylist()?.getSongs()?.let {
                recyclerViewAdapterSongs.updateList(it.toList())
            }
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
        }
    }

    override fun onStart() {
        super.onStart()
        val randomPlaylist: RandomPlaylist? = viewModelUserPicks.getUserPickedPlaylist()
        if (randomPlaylist != null) {
            setUpBroadcastReceiver(randomPlaylist)
        }
    }

    private fun setUpBroadcastReceiver(randomPlaylist: RandomPlaylist) {
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(
            requireActivity().resources.getString(
                R.string.broadcast_receiver_action_service_connected
            )
        )
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action: String? = intent.action
                if (action != null) {
                    if (action == resources.getString(
                            R.string.broadcast_receiver_action_service_connected
                        )
                    ) {
                        setUpRecyclerView(randomPlaylist)
                    }
                }
            }
        }
        requireActivity().registerReceiver(broadcastReceiver, filterComplete)
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
            // userPickedSongs.isEmpty() when the user is editing a playlist
            // TODO got interrupted by food being done
            viewModelUserPicks.fragmentPlaylistFABClicked(
                NavHostFragment.findNavController(this)
            )
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).isVisible = true
        menu.getItem(ActivityMain.MENU_ACTION_LOWER_PROBS_INDEX).isVisible = true
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

    override fun onMenuItemClickAddToPlaylist(song: Song): Boolean {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song)
        val dialogFragment: DialogFragment = DialogFragmentAddToPlaylist()
        dialogFragment.arguments = bundle
        dialogFragment.show(parentFragmentManager, tag)
        return true
    }

    override fun onMenuItemClickAddToQueue(song: Song): Boolean {
        // TODO fix how music continues after queue is depleted
        // turn shuffle and looping off
        // TODO showSongPane?
        viewModelAddToQueue.addToQueueClicked(requireActivity().applicationContext, song)
        return true
    }

    override fun onClickViewHolder(song: Song) {
        // TODO make sure user picked playlist and queue is updated
        // Should queue be cleared?
        viewModelUserPicks.songClicked(
            requireActivity().applicationContext,
            NavHostFragment.findNavController(this),
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
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            val itemSearch = menu.findItem(R.id.action_search)
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(null)
            searchView.onActionViewCollapsed()
        }
        viewModelActivityMain.setFabOnClickListener(null)
        viewModelAddToQueue.setPlaylistToAddToQueue(null)
    }

}
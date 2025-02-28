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
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import io.fourth_finger.pinky_player.KeyboardUtil
import io.fourth_finger.pinky_player.R
import io.fourth_finger.pinky_player.activity_main.ActivityMain
import io.fourth_finger.pinky_player.activity_main.DialogFragmentAddToPlaylist
import io.fourth_finger.pinky_player.activity_main.MenuActionIndex
import io.fourth_finger.pinky_player.activity_main.ViewModelActivityMain
import io.fourth_finger.pinky_player.databinding.RecyclerViewSongListBinding
import io.fourth_finger.playlist_data_source.Song
import java.util.Locale

class FragmentQueue : Fragment(), RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>{
        ViewModelActivityMain.Factory
    }

    private val viewModelFragmentQueue by activityViewModels<ViewModelFragmentQueue>{
        ViewModelFragmentQueue.Factory
    }

    private var broadcastReceiver: BroadcastReceiver? = null

    private lateinit var recyclerViewAdapterSongs: RecyclerViewAdapterSongs

    var dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModelFragmentQueue.songQueue.observe(viewLifecycleOwner){
            if(::recyclerViewAdapterSongs.isInitialized) {
                recyclerViewAdapterSongs.updateList(it.toList())
            }
        }
        _binding = RecyclerViewSongListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setUpMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {
                menuInflater.inflate(
                    R.menu.menu_toolbar,
                    menu
                )
                for (menuActionIndex in MenuActionIndex.entries) {
                    val menuItem = menu.getItem(menuActionIndex.ordinal)
                    when (menuActionIndex) {
                        MenuActionIndex.MENU_ACTION_ADD_TO_PLAYLIST_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_QUEUE_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_SEARCH_INDEX -> {
                            menuItem.isVisible = true
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

                        MenuActionIndex.MENU_ACTION_ADD_TO_QUEUE_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_LOWER_PROBABILITIES_INDEX -> {
                            menuItem.isVisible = false
                        }

                        MenuActionIndex.MENU_ACTION_RESET_PROBABILITIES_INDEX -> {
                            menuItem.isVisible = false
                        }
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtil.hideKeyboard(view)
        setUpMenu()

        // TODO why is this set up twice in all the fragments?!
        // I think due to incomplete state
        setUpRecyclerView()
    }

    private fun siftSongs(newText: String) {
        if (newText.isNotEmpty()) {
            val sifted = siftQueueSongs(newText)
            recyclerViewAdapterSongs.updateList(sifted)
        } else {
            viewModelFragmentQueue.songQueue.value!!.toList().let {
                recyclerViewAdapterSongs.updateList(it)
            }
        }
    }

    private fun siftQueueSongs(string: String): List<Song> {
        val songs: List<Song>? = viewModelFragmentQueue.songQueue.value!!.toList()
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

    private fun setUpRecyclerView() {
        val recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList.layoutManager = LinearLayoutManager(recyclerViewSongList.context)
        recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
            this,
            viewModelFragmentQueue.songQueue.value!!.toList()
        )
        recyclerViewSongList.adapter = recyclerViewAdapterSongs
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
                viewModelFragmentQueue.notifySongMoved(
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
                if(viewModelFragmentQueue.notifySongRemoved(position)){
                    val context = requireActivity().applicationContext
                    viewModelActivityMain.playPauseClicked(context)
                    viewModelActivityMain.playNext(context)
                }
                recyclerViewAdapterSongs.updateList(viewModelFragmentQueue.songQueue.value!!.toList())
                val snackBar: Snackbar = Snackbar.make(
                    binding.recyclerViewSongList,
                    R.string.song_removed,
                    BaseTransientBottomBar.LENGTH_LONG
                )
                snackBar.setAction(R.string.undo) {
                    viewModelFragmentQueue.notifyItemInserted(position)
                    recyclerViewAdapterSongs.updateList(viewModelFragmentQueue.songQueue.value!!.toList())
                }
                snackBar.show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerViewSongList)
    }

    override fun onStart() {
        super.onStart()
        setUpBroadcastReceiver()
    }

    private fun setUpBroadcastReceiver() {
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
                        setUpRecyclerView()
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(broadcastReceiver, filterComplete, Service.RECEIVER_EXPORTED)
        } else {
            requireActivity().registerReceiver(broadcastReceiver, filterComplete)
        }    }

    override fun onResume() {
        super.onResume()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.queue))
        updateFAB()
    }

    private fun updateFAB() {
        viewModelActivityMain.showFab(false)
    }

    override fun onContextMenuItemClickAddToPlaylist(song: Song) {
        val bundle = Bundle()
        bundle.putSerializable(DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, song)
        bundle.putSerializable(
            DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST,
            null
        )
        val dialogFragment: DialogFragment = DialogFragmentAddToPlaylist()
        dialogFragment.arguments = bundle
        dialogFragment.show(parentFragmentManager, tag)
    }

    override fun onContextMenuItemClickAddToQueue(song: Song) {
        viewModelActivityMain.addToQueue(requireActivity().applicationContext, song)
    }

    override fun onClickViewHolder(pos: Int, song: Song) {
        viewModelActivityMain.queueSongClicked(
            this,
            pos
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

}
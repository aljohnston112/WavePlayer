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
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.fourthFinger.pinkyPlayer.KeyboardUtil
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.activity_main.ActivityMain
import io.fourthFinger.pinkyPlayer.activity_main.DialogFragmentAddToPlaylist
import io.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import io.fourthFinger.pinkyPlayer.databinding.RecyclerViewPlaylistListBinding
import io.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class FragmentPlaylists : Fragment(), RecyclerViewAdapterPlaylists.ListenerCallbackPlaylists {

    private var _binding: RecyclerViewPlaylistListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>{
        ViewModelActivityMain.Factory
    }
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>{
        ViewModelPlaylists.Factory
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapterPlaylists: RecyclerViewAdapterPlaylists

    var dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RecyclerViewPlaylistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtil.hideKeyboard(requireView())
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.playlists))
        setUpRecyclerView()
        setSearchView()
    }

    private fun setUpRecyclerView() {
        recyclerView = binding.recyclerViewPlaylistList
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerViewAdapterPlaylists = RecyclerViewAdapterPlaylists(
            this,
            viewModelPlaylists.getPlaylists()
        )
        recyclerView.adapter = recyclerViewAdapterPlaylists
        ItemTouchHelper(object : ItemTouchHelper.Callback() {
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
                viewModelPlaylists.notifyPlaylistMoved(
                    requireActivity().applicationContext,
                    viewHolder.absoluteAdapterPosition,
                    target.absoluteAdapterPosition
                )
                recyclerViewAdapterPlaylists.notifyItemMoved(
                    viewHolder.absoluteAdapterPosition,
                    target.absoluteAdapterPosition
                )
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position: Int = viewHolder.absoluteAdapterPosition
                viewModelPlaylists.notifyPlaylistRemoved(
                    requireActivity().applicationContext,
                    position
                )
                recyclerViewAdapterPlaylists.updateList(viewModelPlaylists.getPlaylists())
                val snackBar: Snackbar = Snackbar.make(
                    binding.recyclerViewPlaylistList,
                    R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG
                )
                snackBar.setAction(R.string.undo) {
                    viewModelPlaylists.notifyPlaylistInserted(
                        requireActivity().applicationContext,
                        position
                    )
                    recyclerViewAdapterPlaylists.updateList(viewModelPlaylists.getPlaylists())
                }
                snackBar.show()
            }
        }).attachToRecyclerView(recyclerView)
    }


    private fun setSearchView() {
        val searchView: SearchView = requireActivity().findViewById<Toolbar>(
            R.id.toolbar
        ).menu?.findItem(R.id.action_search)?.actionView as SearchView
        if (searchView.query.isNotEmpty()) {
            filterPlaylists(searchView.query.toString())
        }
    }

    private fun filterPlaylists(newText: String) {
        val sifted = viewModelPlaylists.siftPlaylists(newText)
        dragFlags = if (newText.isNotEmpty()) {
            recyclerViewAdapterPlaylists.updateList(sifted)
            0
        } else {
            recyclerViewAdapterPlaylists.updateList(viewModelPlaylists.getPlaylists())
            ItemTouchHelper.UP or ItemTouchHelper.DOWN
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
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
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
        viewModelActivityMain.setFabImage(R.drawable.ic_add_black_24dp)
        viewModelActivityMain.setFABText(R.string.fab_new)
        viewModelActivityMain.showFab(true)
        viewModelActivityMain.setFabOnClickListener {
            // userPickedPlaylist is null when user is making a new playlist
            viewModelActivityMain.currentContextPlaylist = null
            viewModelPlaylists.fabClicked(
                NavHostFragment.findNavController(this)
            )
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).isVisible = true
        val itemSearch = menu.findItem(R.id.action_search)
        if (itemSearch != null) {
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    filterPlaylists(newText)
                    return true
                }
            })
        }
    }

    override fun onMenuItemClickAddToPlaylist(randomPlaylist: RandomPlaylist) {
        val bundle = Bundle()
        bundle.putSerializable(
            DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST,
            randomPlaylist
        )
        val dialogFragmentAddToPlaylist = DialogFragmentAddToPlaylist()
        dialogFragmentAddToPlaylist.arguments = bundle
        dialogFragmentAddToPlaylist.show(parentFragmentManager, tag)
    }

    override fun onMenuItemClickAddToQueue(randomPlaylist: RandomPlaylist) {
        viewModelActivityMain.addToQueue(
            requireActivity().applicationContext,
            randomPlaylist
        )
    }

    override fun onClickViewHolder(randomPlaylist: RandomPlaylist) {
        viewModelActivityMain.currentContextPlaylist = randomPlaylist
        viewModelPlaylists.playlistClicked(
            NavHostFragment.findNavController(this),
            randomPlaylist
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Should this be in onDestroyView like was changed in FragmentPlaylist
    override fun onDestroy() {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        super.onDestroy()
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
package com.fourthFinger.pinkyPlayer.fragments

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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavDirections
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
import com.fourthFinger.pinkyPlayer.media_controller.MediaModel
import com.fourthFinger.pinkyPlayer.media_controller.MediaData
import com.fourthFinger.pinkyPlayer.media_controller.MediaPlayerModel
import com.fourthFinger.pinkyPlayer.random_playlist.AudioUri
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.util.*

class FragmentPlaylist : Fragment(), RecyclerViewAdapterSongs.ListenerCallbackSongs {

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelPlaylists by activityViewModels<ViewModelPlaylists>()
    private val mediaPlayerModel = MediaPlayerModel.getInstance()

    private val songQueue = SongQueue.getInstance()

    private var broadcastReceiver: BroadcastReceiver? = null

    private var recyclerViewSongList: RecyclerView? = null
    private var recyclerViewAdapterSongs: RecyclerViewAdapterSongs? = null

    private lateinit var mediaData: MediaData
    private lateinit var mediaModel: MediaModel

    var dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    val swipeFlags: Int = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaModel = MediaModel.getInstance(requireActivity().applicationContext)
        mediaData = MediaData.getInstance()

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = RecyclerViewSongListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtil.hideKeyboard(view)
        val randomPlaylist: RandomPlaylist? = viewModelPlaylists.getUserPickedPlaylist()
        if (randomPlaylist != null) {
            viewModelActivityMain.setPlaylistToAddToQueue(randomPlaylist)
            viewModelActivityMain.setActionBarTitle(randomPlaylist.getName())
            setUpRecyclerView(randomPlaylist)
        }
    }

    private fun setUpBroadcastReceiver(randomPlaylist: RandomPlaylist) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        val filterComplete = IntentFilter()
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT)
        filterComplete.addAction(activityMain.resources.getString(
                R.string.broadcast_receiver_action_on_create_options_menu))
        filterComplete.addAction(activityMain.resources.getString(
                R.string.broadcast_receiver_action_service_connected))
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action: String? = intent.action
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
        val toolbar: Toolbar = activityMain.findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).isVisible = true
            menu.getItem(ActivityMain.MENU_ACTION_LOWER_PROBS_INDEX).isVisible = true
            menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).isVisible = true
            menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).isVisible = true
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).isVisible = true
            val itemSearch = menu.findItem(R.id.action_search)
            if (itemSearch != null) {
                val onQueryTextListener = object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        filterSongs(newText)
                        return true
                    }
                }
                val searchView = itemSearch.actionView as SearchView
                searchView.setOnQueryTextListener(null)
                searchView.setOnQueryTextListener(onQueryTextListener)
            }
        }
    }

    private fun filterSongs(newText: String?) {
        // TODO fix bug where you can reorder songs when sifted
        val songs: List<Song>? = viewModelPlaylists.getUserPickedPlaylist()?.getSongs()
        val sifted: MutableList<Song> = ArrayList<Song>()
        if (songs != null) {
            dragFlags = if (newText != null && newText != "") {
                for (song in songs) {
                    if (song.title.toLowerCase(Locale.ROOT).contains(newText.toLowerCase(Locale.ROOT))) {
                        sifted.add(song)
                    }
                }
                recyclerViewAdapterSongs?.updateList(sifted)
                0

            } else {
                recyclerViewAdapterSongs?.updateList(songs)
                ItemTouchHelper.UP or ItemTouchHelper.DOWN
            }
        }
    }

    private fun setUpRecyclerView(userPickedPlaylist: RandomPlaylist) {
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList?.layoutManager = LinearLayoutManager(recyclerViewSongList?.context)
        recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
                this,
                userPickedPlaylist.getSongs()
        )
        recyclerViewSongList?.adapter = recyclerViewAdapterSongs
        val itemTouchHelperCallback = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView,
                                          viewHolder: RecyclerView.ViewHolder): Int {

                return makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                // TODO hopefully not needed
                /*
                    Collections.swap(recyclerViewAdapterSongs.getSongs(),
                            viewHolder.getAdapterPosition(), target.getAdapterPosition());

                 */
                userPickedPlaylist.swapSongPositions(
                        viewHolder.adapterPosition, target.adapterPosition)
                recyclerViewAdapterSongs?.notifyItemMoved(
                        viewHolder.adapterPosition, target.adapterPosition)
                activityMain.saveFile()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position: Int = viewHolder.adapterPosition
                val song: Song = userPickedPlaylist.getSongs()[position]
                val probability: Double = userPickedPlaylist.getProbability(song)
                if (userPickedPlaylist.size() == 1) {
                    viewModelPlaylists.removePlaylist(userPickedPlaylist)
                    viewModelPlaylists.setUserPickedPlaylist(null)
                } else {
                    userPickedPlaylist.remove(song)
                }
                // TODO needed?
                /*
                recyclerViewAdapterSongs.getSongs().remove(position);
                 */
                recyclerViewAdapterSongs?.notifyItemRemoved(position)
                activityMain.saveFile()
                val snackbar: Snackbar = Snackbar.make(
                        binding.recyclerViewSongList,
                        R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG)
                snackbar.setAction(R.string.undo) {
                    userPickedPlaylist.add(song, probability)
                    userPickedPlaylist.switchSongPositions(userPickedPlaylist.size() - 1, position)
                    // TODO needed?
                    /*
                    recyclerViewAdapterSongs.updateList(userPickedPlaylist.getSongs());
                 */
                    recyclerViewAdapterSongs?.notifyDataSetChanged()
                    activityMain.saveFile()
                }
                snackbar.show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerViewSongList)
        val searchView: SearchView = activityMain.findViewById<Toolbar>(
            R.id.toolbar
        ).menu?.findItem(R.id.action_search)?.actionView as SearchView
        if(searchView.query.isNotEmpty()){
            filterSongs(searchView.query.toString())
        }
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
        viewModelActivityMain.setFabOnClickListener {
            // userPickedSongs.isEmpty() when the user is editing a playlist
            viewModelPlaylists.clearUserPickedSongs()
            NavHostFragment.findNavController(this).navigate(
                    FragmentPlaylistDirections.actionFragmentPlaylistToFragmentEditPlaylist())
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
        // shuffle is off and looping is on or something like that?
        val activityMain: ActivityMain = requireActivity() as ActivityMain
        songQueue.addToQueue(song.id)
        if (!mediaModel.isSongInProgress()) {
            activityMain.showSongPane()
            mediaModel.playNext(requireActivity().applicationContext)
        }
        return true
    }

    override fun onClickViewHolder(song: Song) {
        synchronized(ActivityMain.MUSIC_CONTROL_LOCK) {
            if (song == mediaPlayerModel.currentAudioUri.value?.id?.let {
                        viewModelPlaylists.getSong(it)
                    }
            ) {
                mediaModel.seekTo(requireActivity().applicationContext, 0)
            }
            viewModelPlaylists.getUserPickedPlaylist()?.let { mediaModel.setCurrentPlaylist(it) }
            songQueue.clearSongQueue()
            songQueue.addToQueue(song.id)
            val audioUri = AudioUri.getAudioUri(requireContext(), song.id)
            if(audioUri != null) {
                mediaPlayerModel.setCurrentAudioUri(audioUri)
            }
            mediaModel.playNext(requireActivity().applicationContext)
        }
        val action: NavDirections = FragmentPlaylistDirections.actionFragmentPlaylistToFragmentSong()
        NavHostFragment.findNavController(this).navigate(action)
    }

    override fun onStart() {
        super.onStart()
        val randomPlaylist: RandomPlaylist? = viewModelPlaylists.getUserPickedPlaylist()
        if (randomPlaylist != null) {
            setUpBroadcastReceiver(randomPlaylist)
        }
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
        val toolbar: Toolbar = requireActivity().findViewById(R.id.toolbar)
        val menu = toolbar.menu
        if (menu != null) {
            val itemSearch = menu.findItem(R.id.action_search)
            val searchView = itemSearch.actionView as SearchView
            searchView.setOnQueryTextListener(null)
            searchView.onActionViewCollapsed()
        }
        viewModelActivityMain.setFabOnClickListener(null)
        viewModelActivityMain.setPlaylistToAddToQueue(null)
    }
}
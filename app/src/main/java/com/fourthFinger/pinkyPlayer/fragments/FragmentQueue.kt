package com.fourthFinger.pinkyPlayer.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fourthFinger.pinkyPlayer.KeyboardUtil
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.activity_main.DialogFragmentAddToPlaylist
import com.fourthFinger.pinkyPlayer.activity_main.ViewModelActivityMain
import com.fourthFinger.pinkyPlayer.databinding.RecyclerViewSongListBinding
import com.fourthFinger.pinkyPlayer.random_playlist.MediaSession
import com.fourthFinger.pinkyPlayer.random_playlist.Song
import com.fourthFinger.pinkyPlayer.random_playlist.SongQueue
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class FragmentQueue : Fragment(), RecyclerViewAdapterSongs.ListenerCallbackSongs {

    // TODO update RecyclerView when a new song is added to the queue
    // TODO stop playback of song when removed from queue and start the next

    private var _binding: RecyclerViewSongListBinding? = null
    private val binding get() = _binding!!

    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()
    private val viewModelAddToQueue by activityViewModels<ViewModelAddToQueue>()
    private val viewModelFragmentQueue by activityViewModels<ViewModelFragmentQueue>()

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
        val songQueue = SongQueue.getInstance()
        songQueue.songQueue.observe(viewLifecycleOwner){
            if(::recyclerViewAdapterSongs.isInitialized) {
                recyclerViewAdapterSongs.updateList(it.toList())
            }
        }
        _binding = RecyclerViewSongListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        KeyboardUtil.hideKeyboard(view)
        // TODO why is this set up twice in all the fragments?!
        // I think due to incomplete state
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        val songQueue = SongQueue.getInstance()
        recyclerViewSongList = binding.recyclerViewSongList
        recyclerViewSongList?.layoutManager = LinearLayoutManager(recyclerViewSongList?.context)
        recyclerViewAdapterSongs = RecyclerViewAdapterSongs(
            this,
            songQueue.queue()
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
                songQueue.notifySongMoved(
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
                if(songQueue.notifySongRemoved(position)){
                    val context = requireActivity().applicationContext
                    val mediaSession = MediaSession.getInstance(context)
                    mediaSession.pauseOrPlay(context)
                    mediaSession.playNext(context)
                }
                recyclerViewAdapterSongs.updateList(songQueue.queue().toList())
                val snackBar: Snackbar = Snackbar.make(
                    binding.recyclerViewSongList,
                    R.string.song_removed,
                    BaseTransientBottomBar.LENGTH_LONG
                )
                snackBar.setAction(R.string.undo) {
                    songQueue.notifyItemInserted(position)
                    recyclerViewAdapterSongs.updateList(songQueue.queue().toList())
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
        requireActivity().registerReceiver(broadcastReceiver, filterComplete)
    }

    override fun onResume() {
        super.onResume()
        viewModelActivityMain.setActionBarTitle(resources.getString(R.string.queue))
        updateFAB()
        setUpRecyclerView()
    }

    private fun updateFAB() {
        viewModelActivityMain.showFab(false)
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

    override fun onClickViewHolder(pos: Int, song: Song) {
        // TODO make sure user picked playlist and queue is updated
        // Should queue be cleared?
        viewModelFragmentQueue.songClicked(
            requireActivity().applicationContext,
            this,
            pos
        )
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

}
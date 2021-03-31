package com.example.waveplayer.fragments

import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.waveplayer.R
import com.example.waveplayer.random_playlist.RandomPlaylist

class RecyclerViewAdapterPlaylists(private var listenerCallbackPlaylists: ListenerCallbackPlaylists?,
                                   private var randomPlaylists: MutableList<RandomPlaylist?>?) : RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder?>() {
    interface ListenerCallbackPlaylists {
        open fun onClickViewHolder(randomPlaylist: RandomPlaylist?)
        open fun onMenuItemClickAddToPlaylist(randomPlaylist: RandomPlaylist?): Boolean
        open fun onMenuItemClickAddToQueue(randomPlaylist: RandomPlaylist?): Boolean
    }

    private var onCreateContextMenuListenerPlaylists: OnCreateContextMenuListener? = null
    private var onMenuItemClickListenerAddToPlaylist: MenuItem.OnMenuItemClickListener? = null
    private var onMenuItemClickListenerAddToQueue: MenuItem.OnMenuItemClickListener? = null
    private var onClickListenerHandle: View.OnClickListener? = null
    private var onClickListenerViewHolder: View.OnClickListener? = null
    fun updateList(randomPlaylists: MutableList<RandomPlaylist?>?) {
        this.randomPlaylists = randomPlaylists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder.randomPlaylist = randomPlaylists.get(position)
        holder.textViewPlaylistName.setText(randomPlaylists.get(position).getName())
        onMenuItemClickListenerAddToPlaylist = MenuItem.OnMenuItemClickListener { menuItem: MenuItem? -> listenerCallbackPlaylists.onMenuItemClickAddToPlaylist(holder.randomPlaylist) }
        onMenuItemClickListenerAddToQueue = MenuItem.OnMenuItemClickListener { menuItem2: MenuItem? -> listenerCallbackPlaylists.onMenuItemClickAddToQueue(holder.randomPlaylist) }
        onCreateContextMenuListenerPlaylists = OnCreateContextMenuListener { menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo? ->
            val itemAddToPlaylist = menu.add(R.string.add_to_playlist)
            itemAddToPlaylist.setOnMenuItemClickListener(onMenuItemClickListenerAddToPlaylist)
            val itemAddToQueue = menu.add(R.string.add_to_queue)
            itemAddToQueue.setOnMenuItemClickListener(onMenuItemClickListenerAddToQueue)
        }
        holder.handle.setOnCreateContextMenuListener(null)
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerPlaylists)
        onClickListenerHandle = View.OnClickListener { v: View? -> holder.handle.performLongClick() }
        holder.handle.setOnClickListener(null)
        holder.handle.setOnClickListener(onClickListenerHandle)
        onClickListenerViewHolder = View.OnClickListener { v: View? ->
            if (position != RecyclerView.NO_POSITION) {
                listenerCallbackPlaylists.onClickViewHolder(holder.randomPlaylist)
            }
        }
        holder.playlistView.setOnClickListener(null)
        holder.playlistView.setOnClickListener(onClickListenerViewHolder)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.handle.setOnCreateContextMenuListener(null)
        onCreateContextMenuListenerPlaylists = null
        onMenuItemClickListenerAddToPlaylist = null
        onMenuItemClickListenerAddToQueue = null
        holder.handle.setOnClickListener(null)
        onClickListenerHandle = null
        holder.playlistView.setOnClickListener(null)
        onClickListenerViewHolder = null
        holder.randomPlaylist = null
        listenerCallbackPlaylists = null
    }

    override fun getItemCount(): Int {
        return randomPlaylists.size
    }

    class ViewHolder(val playlistView: View?) : RecyclerView.ViewHolder(playlistView) {
        val textViewPlaylistName: TextView?
        val handle: ImageView?
        var randomPlaylist: RandomPlaylist? = null
        override fun toString(): String {
            return randomPlaylist.getName()
        }

        init {
            textViewPlaylistName = playlistView.findViewById(R.id.text_view_playlist_name)
            if (randomPlaylist != null) {
                textViewPlaylistName.setText(randomPlaylist.getName())
            }
            handle = playlistView.findViewById(R.id.playlist_handle)
        }
    }
}
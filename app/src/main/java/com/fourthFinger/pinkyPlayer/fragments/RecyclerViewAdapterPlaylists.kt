package com.fourthFinger.pinkyPlayer.fragments

import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fourthFinger.pinkyPlayer.DiffUtils
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist

class RecyclerViewAdapterPlaylists(
        private var listenerCallbackPlaylists: ListenerCallbackPlaylists,
        randomPlaylists: List<RandomPlaylist>
) : ListAdapter<RandomPlaylist, RecyclerViewAdapterPlaylists.ViewHolder>(
    DiffUtils.DiffUtilItemCallbackPlaylists()
) {

    interface ListenerCallbackPlaylists {
        fun onClickViewHolder(randomPlaylist: RandomPlaylist)
        fun onMenuItemClickAddToPlaylist(randomPlaylist: RandomPlaylist): Boolean
        fun onMenuItemClickAddToQueue(randomPlaylist: RandomPlaylist): Boolean
    }

    fun updateList(randomPlaylists: List<RandomPlaylist>) {
        submitList(ArrayList(randomPlaylists))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.item_playlist,
            parent,
            false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.randomPlaylist = getItem(position)
        holder.textViewPlaylistName.text = getItem(position).getName()
        holder.handle.setOnCreateContextMenuListener{ menu: ContextMenu?, _: View?, _: ContextMenuInfo? ->
            val itemAddToPlaylist = menu?.add(R.string.add_to_playlist)
            itemAddToPlaylist?.setOnMenuItemClickListener{
                listenerCallbackPlaylists.onMenuItemClickAddToPlaylist(getItem(position))
            }
            val itemAddToQueue = menu?.add(R.string.add_to_queue)
            itemAddToQueue?.setOnMenuItemClickListener{
                listenerCallbackPlaylists.onMenuItemClickAddToQueue(getItem(position))
            }
        }
        holder.handle.setOnClickListener{ holder.handle.performLongClick() }
        holder.playlistView.setOnClickListener{
            if (position != RecyclerView.NO_POSITION) {
                listenerCallbackPlaylists.onClickViewHolder(getItem(position))
            }
        }
    }

    init {
        updateList(randomPlaylists)
    }

    class ViewHolder(val playlistView: View) : RecyclerView.ViewHolder(playlistView) {
        val textViewPlaylistName: TextView = playlistView.findViewById(R.id.text_view_playlist_name)
        val handle: ImageView = playlistView.findViewById(R.id.playlist_handle)

        lateinit var randomPlaylist: RandomPlaylist

        override fun toString(): String {
            return randomPlaylist.getName()
        }

    }

}
package io.fourth_finger.pinky_player.fragments

import android.annotation.SuppressLint
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.fourth_finger.pinky_player.R
import io.fourth_finger.playlist_data_source.RandomPlaylist

class RecyclerViewAdapterPlaylists(
    private var listenerCallbackPlaylists: ListenerCallbackPlaylists,
    private var randomPlaylists: List<RandomPlaylist>
) : RecyclerView.Adapter<RecyclerViewAdapterPlaylists.ViewHolder>() {


    interface ListenerCallbackPlaylists {
        fun onClickViewHolder(randomPlaylist: RandomPlaylist)
        fun onMenuItemClickAddToPlaylist(randomPlaylist: RandomPlaylist)
        fun onMenuItemClickAddToQueue(randomPlaylist: RandomPlaylist)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(randomPlaylists: List<RandomPlaylist>) {
        this.randomPlaylists = randomPlaylists
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_playlist,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.randomPlaylist = randomPlaylists[position]
        holder.textViewPlaylistName.text = randomPlaylists[position].name
        holder.handle.setOnCreateContextMenuListener { menu: ContextMenu?, _: View?, _: ContextMenuInfo? ->
            val itemAddToPlaylist = menu?.add(R.string.add_to_playlist)
            itemAddToPlaylist?.setOnMenuItemClickListener {
                listenerCallbackPlaylists.onMenuItemClickAddToPlaylist(randomPlaylists[position])
                true
            }
            val itemAddToQueue = menu?.add(R.string.add_to_queue)
            itemAddToQueue?.setOnMenuItemClickListener {
                listenerCallbackPlaylists.onMenuItemClickAddToQueue(randomPlaylists[position])
                true
            }
        }
        holder.handle.setOnClickListener { holder.handle.performLongClick() }
        holder.playlistView.setOnClickListener {
                listenerCallbackPlaylists.onClickViewHolder(randomPlaylists[position])
        }
    }

    override fun getItemCount(): Int {
        return randomPlaylists.size
    }

    class ViewHolder(val playlistView: View) : RecyclerView.ViewHolder(playlistView) {
        val textViewPlaylistName: TextView = playlistView.findViewById(R.id.text_view_playlist_name)
        val handle: ImageView = playlistView.findViewById(R.id.playlist_handle)

        lateinit var randomPlaylist: RandomPlaylist

        override fun toString(): String {
            return randomPlaylist.name
        }

    }

}
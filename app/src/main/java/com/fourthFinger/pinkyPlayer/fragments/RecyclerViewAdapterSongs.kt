package com.fourthFinger.pinkyPlayer.fragments

import android.annotation.SuppressLint
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fourthFinger.pinkyPlayer.R
import com.fourthFinger.pinkyPlayer.random_playlist.Song

class RecyclerViewAdapterSongs(
        private val listenerCallbackSongs: ListenerCallbackSongs,
        private var songs: List<Song>
) : RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder>() {

    interface ListenerCallbackSongs {
        fun onClickViewHolder(song: Song)
        fun onMenuItemClickAddToPlaylist(song: Song): Boolean
        fun onMenuItemClickAddToQueue(song: Song): Boolean
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(songs: List<Song>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_song, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.song = songs[position]
        holder.handle.setOnClickListener { holder.handle.performLongClick() }
        holder.songView.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                listenerCallbackSongs.onClickViewHolder(holder.song)
            }
        }
        holder.textViewSongName.text = songs[position].title
        holder.handle.setOnCreateContextMenuListener{ menu: ContextMenu?, _: View?, _: ContextMenuInfo? ->
            val menuItemAddToPlaylist = menu?.add(R.string.add_to_playlist)
            menuItemAddToPlaylist?.setOnMenuItemClickListener {
                listenerCallbackSongs.onMenuItemClickAddToPlaylist(holder.song)
            }
            val menuItemAddToQueue = menu?.add(R.string.add_to_queue)
            menuItemAddToQueue?.setOnMenuItemClickListener {
                listenerCallbackSongs.onMenuItemClickAddToQueue(holder.song)
            }
        }
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    class ViewHolder(val songView: View) : RecyclerView.ViewHolder(songView) {
        val textViewSongName: TextView = songView.findViewById(R.id.text_view_songs_name)
        val handle: ImageView = songView.findViewById(R.id.song_handle)
        lateinit var song: Song

        override fun toString(): String {
            return song.title
        }

    }
}
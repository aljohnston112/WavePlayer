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
import io.fourth_finger.playlist_data_source.Song

class RecyclerViewAdapterSongs(
    private val listenerCallbackSongs: ListenerCallbackSongs,
    var songs: List<Song>
) : RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder>() {

    interface ListenerCallbackSongs {
        fun onClickViewHolder(pos: Int, song: Song)
        fun onContextMenuItemClickAddToPlaylist(song: Song)
        fun onContextMenuItemClickAddToQueue(song: Song)
    }


    @SuppressLint("NotifyDataSetChanged")
    fun updateList(songs: List<Song>) {
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_song, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.song = songs[position]
        holder.handle.setOnClickListener { holder.handle.performLongClick() }
        holder.songView.setOnClickListener {
            listenerCallbackSongs.onClickViewHolder(position, holder.song)
        }
        holder.textViewSongName.text = songs[position].title
        holder.handle.setOnCreateContextMenuListener { contextMenu: ContextMenu?,
                                                       _: View?,
                                                       _: ContextMenuInfo? ->
            val contextMenuItemAddToPlaylist = contextMenu?.add(R.string.add_to_playlist)
            contextMenuItemAddToPlaylist?.setOnMenuItemClickListener {
                listenerCallbackSongs.onContextMenuItemClickAddToPlaylist(holder.song)
                true
            }
            val contextMenuItemAddToQueue = contextMenu?.add(R.string.add_to_queue)
            contextMenuItemAddToQueue?.setOnMenuItemClickListener {
                listenerCallbackSongs.onContextMenuItemClickAddToQueue(holder.song)
                true
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
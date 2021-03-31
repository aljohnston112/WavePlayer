package com.example.waveplayer.fragments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.waveplayer.R
import com.example.waveplayer.random_playlist.Song

class RecyclerViewAdapterSelectSongs(private val listenerCallbackSelectSongs: ListenerCallbackSelectSongs?,
                                     private var allSongs: MutableList<Song?>?) : RecyclerView.Adapter<RecyclerViewAdapterSelectSongs.ViewHolder?>() {
    interface ListenerCallbackSelectSongs {
        open fun getUserPickedSongs(): MutableList<Song?>?
        open fun removeUserPickedSong(song: Song?)
        open fun addUserPickedSong(song: Song?)
    }

    fun updateList(songs: MutableList<Song?>?) {
        allSongs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_song, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val linearLayout: ConstraintLayout = holder.songView.findViewById(R.id.constraint_layout_song_name)
        val userPickedSongs = listenerCallbackSelectSongs.getUserPickedSongs()
        if (userPickedSongs.contains(allSongs.get(position))) {
            allSongs.get(position).setSelected(true)
            // TODO use color resources
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#575757"))
            linearLayout.setBackgroundColor(Color.parseColor("#575757"))
        } else {
            allSongs.get(position).setSelected(false)
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#000000"))
            linearLayout.setBackgroundColor(Color.parseColor("#000000"))
        }
        holder.song = allSongs.get(position)
        holder.textViewSongName.setText(allSongs.get(position).title)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        /*
        holder.songView.setOnClickListener(null);
        onClickListener = null;
        holder.song = null;
        listenerCallbackSelectSongs = null;

         */
    }

    override fun getItemCount(): Int {
        return allSongs.size
    }

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view) {
        val songView: View?
        val textViewSongName: TextView?
        var song: Song? = null

        init {
            // TODO invisible or gone?
            view.findViewById<View?>(R.id.song_handle).visibility = View.GONE
            songView = view
            textViewSongName = view.findViewById(R.id.text_view_songs_name)
            val constraintLayout: ConstraintLayout = view.findViewById(R.id.constraint_layout_song_name)
            // TODO color resources
            val onClickListener = View.OnClickListener { v: View? ->
                if (song.isSelected()) {
                    song.setSelected(false)
                    // TODO color resources
                    textViewSongName.setBackgroundColor(Color.parseColor("#000000"))
                    constraintLayout.setBackgroundColor(Color.parseColor("#000000"))
                    listenerCallbackSelectSongs.removeUserPickedSong(song)
                } else {
                    song.setSelected(true)
                    textViewSongName.setBackgroundColor(Color.parseColor("#575757"))
                    constraintLayout.setBackgroundColor(Color.parseColor("#575757"))
                    listenerCallbackSelectSongs.addUserPickedSong(song)
                }
            }
            view.setOnClickListener(onClickListener)
        }
    }
}
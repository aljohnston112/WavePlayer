package io.fourthFinger.pinkyPlayer.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.fourthFinger.pinkyPlayer.R
import io.fourthFinger.pinkyPlayer.random_playlist.Song

class RecyclerViewAdapterSelectSongs(
    private val listenerCallbackSelectSongs: ListenerCallbackSelectSongs,
    private var allSongs: List<Song>
) : RecyclerView.Adapter<RecyclerViewAdapterSelectSongs.ViewHolder>() {

    interface ListenerCallbackSelectSongs {
        fun songUnselected(song: Song)
        fun songSelected(song: Song)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(songs: List<Song>) {
        allSongs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_song, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val linearLayout: ConstraintLayout = holder.itemView.findViewById(
            R.id.constraint_layout_song_name
        )
        // TODO use color resources
        if (allSongs[position].isSelected()) {
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#575757"))
            linearLayout.setBackgroundColor(Color.parseColor("#575757"))
        } else {
            holder.textViewSongName.setBackgroundColor(Color.parseColor("#000000"))
            linearLayout.setBackgroundColor(Color.parseColor("#000000"))
        }
        holder.song = allSongs[position]
        holder.textViewSongName.text = allSongs[position].title
    }

    override fun getItemCount(): Int {
        return allSongs.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewSongName: TextView = view.findViewById(R.id.text_view_songs_name)
        lateinit var song: Song

        init {
            // TODO invisible or gone?
            view.findViewById<View?>(R.id.song_handle).visibility = View.GONE
            val constraintLayout: ConstraintLayout =
                view.findViewById(R.id.constraint_layout_song_name)
            // TODO color resources
            val onClickListener = View.OnClickListener { v: View? ->
                if (song.isSelected()) {
                    song.setSelected(false)
                    // TODO color resources
                    textViewSongName.setBackgroundColor(Color.parseColor("#000000"))
                    constraintLayout.setBackgroundColor(Color.parseColor("#000000"))
                    listenerCallbackSelectSongs.songUnselected(song)
                } else {
                    song.setSelected(true)
                    textViewSongName.setBackgroundColor(Color.parseColor("#575757"))
                    constraintLayout.setBackgroundColor(Color.parseColor("#575757"))
                    listenerCallbackSelectSongs.songSelected(song)
                }
            }
            view.setOnClickListener(onClickListener)
        }
    }

}
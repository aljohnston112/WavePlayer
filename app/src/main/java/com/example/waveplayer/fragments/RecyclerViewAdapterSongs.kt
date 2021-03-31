package com.example.waveplayer.fragments

import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnCreateContextMenuListener
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.waveplayer.R
import com.example.waveplayer.random_playlist.Song

class RecyclerViewAdapterSongs(private val listenerCallbackSongs: ListenerCallbackSongs?, private var songs: MutableList<Song?>?) : RecyclerView.Adapter<RecyclerViewAdapterSongs.ViewHolder?>() {
    interface ListenerCallbackSongs {
        open fun onClickViewHolder(song: Song?)
        open fun onMenuItemClickAddToPlaylist(song: Song?): Boolean
        open fun onMenuItemClickAddToQueue(song: Song?): Boolean
    }

    private var onCreateContextMenuListenerSongs: OnCreateContextMenuListener? = null
    private var onMenuItemClickListenerAddToPlaylist: MenuItem.OnMenuItemClickListener? = null
    private var onMenuItemClickListenerAddToQueue: MenuItem.OnMenuItemClickListener? = null
    private var onClickListenerViewHolder: View.OnClickListener? = null
    private var onClickListenerHandle: View.OnClickListener? = null
    fun updateList(songs: MutableList<Song?>?) {
        this.songs = songs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {

        // onClickListenerHandle
        onClickListenerHandle = View.OnClickListener { v: View? -> holder.handle.performLongClick() }
        holder.handle.setOnClickListener(null)
        holder.handle.setOnClickListener(onClickListenerHandle)

        // onClickListenerViewHolder
        onClickListenerViewHolder = View.OnClickListener { v: View? ->
            if (position != RecyclerView.NO_POSITION) {
                listenerCallbackSongs.onClickViewHolder(holder.song)
            }
        }
        holder.songView.setOnClickListener(null)
        holder.songView.setOnClickListener(onClickListenerViewHolder)
        holder.song = songs.get(position)
        holder.textViewSongName.setText(songs.get(position).title)

        // onCreateContextMenu
        onMenuItemClickListenerAddToPlaylist = MenuItem.OnMenuItemClickListener { menuItem: MenuItem? -> listenerCallbackSongs.onMenuItemClickAddToPlaylist(holder.song) }
        onMenuItemClickListenerAddToQueue = MenuItem.OnMenuItemClickListener { menuItem2: MenuItem? -> listenerCallbackSongs.onMenuItemClickAddToQueue(holder.song) }
        onCreateContextMenuListenerSongs = OnCreateContextMenuListener { menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo? ->
            val menuItemAddToPlaylist = menu.add(R.string.add_to_playlist)
            menuItemAddToPlaylist.setOnMenuItemClickListener(onMenuItemClickListenerAddToPlaylist)
            val menuItemAddToQueue = menu.add(R.string.add_to_queue)
            menuItemAddToQueue.setOnMenuItemClickListener(onMenuItemClickListenerAddToQueue)
        }
        holder.handle.setOnCreateContextMenuListener(null)
        holder.handle.setOnCreateContextMenuListener(onCreateContextMenuListenerSongs)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        /*
        holder.handle.setOnCreateContextMenuListener(null);
        onCreateContextMenuListenerSongs = null;
        onMenuItemClickListenerAddToPlaylist = null;
        onMenuItemClickListenerAddToQueue = null;
        holder.handle.setOnClickListener(null);
        onClickListenerHandle = null;
        holder.songView.setOnClickListener(null);
        onClickListenerViewHolder = null;
        holder.song = null;
        listenerCallbackSongs = null;
         */
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    class ViewHolder(val songView: View?) : RecyclerView.ViewHolder(songView) {
        val textViewSongName: TextView?
        val handle: ImageView?
        var song: Song? = null
        override fun toString(): String {
            return song.title
        }

        init {
            textViewSongName = songView.findViewById(R.id.text_view_songs_name)
            handle = songView.findViewById(R.id.song_handle)
        }
    }
}
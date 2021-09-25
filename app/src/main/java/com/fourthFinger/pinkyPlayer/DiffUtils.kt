package com.fourthFinger.pinkyPlayer

import androidx.recyclerview.widget.DiffUtil
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.Song

class DiffUtils {

    class DiffUtilItemCallbackPlaylists : DiffUtil.ItemCallback<RandomPlaylist>() {
        override fun areItemsTheSame(oldItem: RandomPlaylist, newItem: RandomPlaylist): Boolean {
            return oldItem.getName() == newItem.getName()
        }

        override fun areContentsTheSame(oldItem: RandomPlaylist, newItem: RandomPlaylist): Boolean {
            return true
        }

    }

    class DiffUtilItemCallbackSongs : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return true
        }

    }

}
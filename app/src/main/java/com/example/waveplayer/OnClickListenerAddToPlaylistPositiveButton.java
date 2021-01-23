package com.example.waveplayer;

import android.content.DialogInterface;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.List;

public class OnClickListenerAddToPlaylistPositiveButton implements DialogInterface.OnClickListener {

    private final List<RandomPlaylist> randomPlaylists;
    private final List<Integer> selectedPlaylistIndices;

    private final boolean isSong;
    private final Song song;
    private final RandomPlaylist randomPlaylist;

    private OnClickListenerAddToPlaylistPositiveButton() {
        throw new UnsupportedOperationException();
    }

    public OnClickListenerAddToPlaylistPositiveButton(
            List<RandomPlaylist> randomPlaylists, List<Integer> selectedPlaylistIndices,
            boolean isSong, Song song, RandomPlaylist randomPlaylist) {
        this.randomPlaylists = randomPlaylists;
        this.selectedPlaylistIndices = selectedPlaylistIndices;
        this.isSong = isSong;
        this.song = song;
        this.randomPlaylist = randomPlaylist;
    }


    public void onClick(DialogInterface dialog, int id) {
        if (isSong && song != null) {
            for (int index : selectedPlaylistIndices) {
                randomPlaylists.get(index).add(song);
            }
        }
        if (!isSong && randomPlaylist != null) {
            for (Song randomPlaylistSong : randomPlaylist.getSongs()) {
                for (int index : selectedPlaylistIndices) {
                    randomPlaylists.get(index).add(randomPlaylistSong);
                }
            }
        }
    }

}
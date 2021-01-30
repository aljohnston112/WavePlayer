package com.example.waveplayer.activity_main;

import android.content.DialogInterface;

import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.List;

public class OnClickListenerAddToPlaylistPositiveButton implements DialogInterface.OnClickListener {

    private final List<Integer> selectedPlaylistIndices;

    private final boolean isSong;
    private final Song song;
    private final RandomPlaylist randomPlaylist;

    private OnClickListenerAddToPlaylistPositiveButton() {
        throw new UnsupportedOperationException();
    }

    public OnClickListenerAddToPlaylistPositiveButton(
            List<Integer> selectedPlaylistIndices,
            boolean isSong, Song song, RandomPlaylist randomPlaylist) {
        this.selectedPlaylistIndices = selectedPlaylistIndices;
        this.isSong = isSong;
        this.song = song;
        this.randomPlaylist = randomPlaylist;
    }


    public void onClick(DialogInterface dialog, int id) {
        if (isSong && song != null) {
            for (int index : selectedPlaylistIndices) {
                MediaData.getInstance().getPlaylists().get(index).add(song);
            }
        }
        if (!isSong && randomPlaylist != null) {
            for (Song randomPlaylistSong : randomPlaylist.getSongs()) {
                for (int index : selectedPlaylistIndices) {
                    MediaData.getInstance().getPlaylists().get(index).add(randomPlaylistSong);
                }
            }
        }
    }

}
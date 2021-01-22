package com.example.waveplayer;

import android.content.DialogInterface;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.List;

public class OnClickListenerAddToPlaylistPositiveButton implements DialogInterface.OnClickListener {

    private final List<RandomPlaylist> randomPlaylists;
    private final List<Integer> selectedPlaylistIndices;

    private final boolean isSong;
    private final AudioUri audioURI;
    private final RandomPlaylist randomPlaylist;

    private OnClickListenerAddToPlaylistPositiveButton() {
        throw new UnsupportedOperationException();
    }

    public OnClickListenerAddToPlaylistPositiveButton(
            List<RandomPlaylist> randomPlaylists, List<Integer> selectedPlaylistIndices,
            boolean isSong, AudioUri audioURI, RandomPlaylist randomPlaylist) {
        this.randomPlaylists = randomPlaylists;
        this.selectedPlaylistIndices = selectedPlaylistIndices;
        this.isSong = isSong;
        this.audioURI = audioURI;
        this.randomPlaylist = randomPlaylist;
    }


    public void onClick(DialogInterface dialog, int id) {
        if (isSong && audioURI != null) {
            for (int index : selectedPlaylistIndices) {
                randomPlaylists.get(index).add(audioURI);
            }
        }
        if (!isSong && randomPlaylist != null) {
            for (AudioUri randomPlaylistAudioUri : randomPlaylist.getSongs()) {
                for (int index : selectedPlaylistIndices) {
                    randomPlaylists.get(index).add(randomPlaylistAudioUri);
                }
            }
        }
    }

}
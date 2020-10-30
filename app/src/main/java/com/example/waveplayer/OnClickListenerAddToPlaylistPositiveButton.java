package com.example.waveplayer;

import android.content.DialogInterface;

import java.util.List;

public class OnClickListenerAddToPlaylistPositiveButton implements DialogInterface.OnClickListener {

    final List<RandomPlaylist> randomPlaylists;
    final List<Integer> selectedPlaylistIndices;

    final boolean isSong;
    final AudioUri audioURI;
    final RandomPlaylist randomPlaylist;

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
            for (AudioUri randomPlaylistAudioUri : randomPlaylist.getAudioUris()) {
                for (int index : selectedPlaylistIndices) {
                    randomPlaylists.get(index).add(randomPlaylistAudioUri);
                }
            }
        }
    }

}

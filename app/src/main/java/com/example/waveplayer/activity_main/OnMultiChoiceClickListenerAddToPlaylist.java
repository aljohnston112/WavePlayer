package com.example.waveplayer.activity_main;

import android.content.DialogInterface;

import java.util.List;

public class OnMultiChoiceClickListenerAddToPlaylist
        implements DialogInterface.OnMultiChoiceClickListener {

    private final List<Integer> selectedPlaylists;

    private OnMultiChoiceClickListenerAddToPlaylist(){
        throw new UnsupportedOperationException();
    }

    public OnMultiChoiceClickListenerAddToPlaylist(List<Integer> selectedPlaylists){
        this.selectedPlaylists = selectedPlaylists;
    }

    @Override
    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
        if (isChecked) {
            selectedPlaylists.add(which);
        } else if (selectedPlaylists.contains(which)) {
            selectedPlaylists.remove(Integer.valueOf(which));
        }
    }

}

package com.example.waveplayer;

import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.List;

public class OnMultiChoiceClickListenerAddToPlaylist
        implements DialogInterface.OnMultiChoiceClickListener {

    final List<Integer> selectedPlaylists;

    public OnMultiChoiceClickListenerAddToPlaylist(final List<Integer> selectedPlaylists){
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

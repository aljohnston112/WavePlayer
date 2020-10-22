package com.example.waveplayer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddToPlaylistDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final Bundle bundle = getArguments();
        final List<RandomPlaylist> randomPlaylists =
                (List<RandomPlaylist>) bundle.getSerializable(
                        RecyclerViewAdapterSongs.PLAYLISTS);
        List<String> titles = new ArrayList<>(randomPlaylists.size());
        for (RandomPlaylist randomPlaylist : randomPlaylists) {
            titles.add(randomPlaylist.getName());
        }
        String[] titlesArray = new String[titles.size()];
        int i = 0;
        for (String title : titles) {
            titlesArray[i++] = title;
        }
        final List<Integer> selectedPlaylists = new ArrayList<>();
        builder.setMultiChoiceItems(titlesArray,
                null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            selectedPlaylists.add(which);
                        } else if (selectedPlaylists.contains(which)) {
                            selectedPlaylists.remove(Integer.valueOf(which));
                        }
                    }
                });
        builder.setTitle(R.string.add_to_playlist)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // TODO
                        AudioURI audioURI = (AudioURI) bundle.getSerializable(
                                RecyclerViewAdapterSongs.ADD_TO_PLAYLIST_SONG);
                        for(int index : selectedPlaylists){
                            randomPlaylists.get(index).getProbFun().add(audioURI);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // TODO
                    }
                });
        return builder.create();
    }
}

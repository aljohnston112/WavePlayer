package com.example.waveplayer.activity_main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;

import java.util.ArrayList;
import java.util.List;

public class DialogFragmentAddToPlaylist extends DialogFragment {

    // TODO get rid of bundles... probably not
    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST = "ADD_TO_PLAYLIST_PLAYLIST";
    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_SONG = "ADD_TO_PLAYLIST_SONG";

    private DialogInterface.OnMultiChoiceClickListener onMultiChoiceClickListener;

    // TODO add a cancel button
    private DialogInterface.OnClickListener onClickListenerAddButton;
    private DialogInterface.OnClickListener onClickListenerNewPlaylistButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity().getApplicationContext());
        builder.setTitle(R.string.add_to_playlist);
        Bundle bundle = getArguments();
        if (bundle != null) {
            List<Integer> selectedPlaylistIndices = new ArrayList<>();
            setUpChoices(builder, selectedPlaylistIndices);
            setUpButtons(builder, bundle, selectedPlaylistIndices);
            return builder.create();
        }
        return null;
    }

    private void setUpChoices(AlertDialog.Builder builder, List<Integer> selectedPlaylistIndices) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        onMultiChoiceClickListener = (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedPlaylistIndices.add(which);
                    } else {
                        selectedPlaylistIndices.remove(Integer.valueOf(which));
                    }
                };
        builder.setMultiChoiceItems(getPlaylistTitles(activityMain.getPlaylists()),
                null, onMultiChoiceClickListener);
    }

    // TODO put in MediaData at some point...
    private String[] getPlaylistTitles(List<RandomPlaylist> randomPlaylists) {
        List<String> titles = new ArrayList<>(randomPlaylists.size());
        for (RandomPlaylist randomPlaylist : randomPlaylists) {
            titles.add(randomPlaylist.getName());
        }
        String[] titlesArray =  new String[titles.size()];
        int i = 0;
        for (String title : titles) {
            titlesArray[i++] = title;
        }
        return titlesArray;
    }

    private void setUpButtons(AlertDialog.Builder builder, Bundle bundle,
                              List<Integer> selectedPlaylistIndices) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        // These are here to prevent code duplication
        Song song = (Song) bundle.getSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG);
        RandomPlaylist randomPlaylist = (RandomPlaylist) bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST);
        onClickListenerAddButton = (dialog, id) -> {
            if (song != null) {
                for (int index : selectedPlaylistIndices) {
                    activityMain.getPlaylists().get(index).add(song);
                }
            }
            if (randomPlaylist != null) {
                for (Song randomPlaylistSong : randomPlaylist.getSongs()) {
                    for (int index : selectedPlaylistIndices) {
                        activityMain.getPlaylists().get(index).add(randomPlaylistSong);
                    }
                }
            }
        };
        builder.setPositiveButton(R.string.add, onClickListenerAddButton);
        onClickListenerNewPlaylistButton = (dialog, which) -> {
            // UserPickedPlaylist need to be null for FragmentEditPlaylist to make a new playlist
            activityMain.setUserPickedPlaylist(null);
            activityMain.clearUserPickedSongs();
            if (song != null) {
                activityMain.addUserPickedSong(song);
            }
            if (randomPlaylist != null) {
                for (Song songInPlaylist : randomPlaylist.getSongs()) {
                    activityMain.addUserPickedSong(songInPlaylist);
                }
            }
            activityMain.navigateTo(R.id.fragmentEditPlaylist);
        };
        builder.setNeutralButton(R.string.new_playlist, onClickListenerNewPlaylistButton);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onMultiChoiceClickListener = null;
        onClickListenerAddButton = null;
        onClickListenerNewPlaylistButton = null;
    }
}

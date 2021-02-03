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

    // TODO get rid of bundles
    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST = "ADD_TO_PLAYLIST_PLAYLIST";
    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_SONG = "ADD_TO_PLAYLIST_SONG";
    public static final String BUNDLE_KEY_IS_SONG = "IS_SONG";

    private DialogInterface.OnMultiChoiceClickListener onMultiChoiceClickListener;

    private DialogInterface.OnClickListener onClickListenerPositiveButton;

    private DialogInterface.OnClickListener onClickListenerNeutralButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity().getApplicationContext());
        Bundle bundle = getArguments();
        if (bundle != null) {
            List<Integer> selectedPlaylistIndices = new ArrayList<>();
            builder.setTitle(R.string.add_to_playlist);
            setUpChoices(builder, selectedPlaylistIndices);
            setUpButtons(builder, bundle, selectedPlaylistIndices);
            return builder.create();
        }
        return null;
    }

    private void setUpChoices(AlertDialog.Builder builder,
                              List<Integer> selectedPlaylistIndices) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        onMultiChoiceClickListener = (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedPlaylistIndices.add(which);
                    } else if (selectedPlaylistIndices.contains(which)) {
                        selectedPlaylistIndices.remove(Integer.valueOf(which));
                    }
                };
        builder.setMultiChoiceItems(
                getPlaylistTitles(activityMain.getPlaylists()),
                null, onMultiChoiceClickListener);
    }

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
        boolean isSong = bundle.getBoolean(BUNDLE_KEY_IS_SONG);
        Song song = (Song) bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_SONG);
        RandomPlaylist randomPlaylist = (RandomPlaylist) bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST);
        onClickListenerPositiveButton = (dialog, id) -> {
            if (isSong && song != null) {
                for (int index : selectedPlaylistIndices) {
                    activityMain.getPlaylists().get(index).add(song);
                }
            }
            if (!isSong && randomPlaylist != null) {
                for (Song randomPlaylistSong : randomPlaylist.getSongs()) {
                    for (int index : selectedPlaylistIndices) {
                        activityMain.getPlaylists().get(index).add(randomPlaylistSong);
                    }
                }
            }
        };
        onClickListenerNeutralButton = (dialog, which) -> {
            // Needed for FragmentEditPlaylist to make a new playlist
            activityMain.setUserPickedPlaylist(null);
            activityMain.clearUserPickedSongs();
            if (isSong && song != null) {
                activityMain.addUserPickedSong(song);
            }
            if (!isSong && randomPlaylist != null) {
                for (Song songInPlaylist : randomPlaylist.getSongs()) {
                    activityMain.addUserPickedSong(songInPlaylist);
                }
            }
            activityMain.navigateTo(R.id.fragmentEditPlaylist);
        };
        builder.setPositiveButton(R.string.add, onClickListenerPositiveButton)
                .setNeutralButton(R.string.new_playlist, onClickListenerNeutralButton);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onMultiChoiceClickListener = null;
        onClickListenerPositiveButton = null;
        onClickListenerNeutralButton = null;
    }
}

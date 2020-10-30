package com.example.waveplayer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class DialogFragmentAddToPlaylist extends DialogFragment {

    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST = "ADD_TO_PLAYLIST_PLAYLIST";
    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_SONG = "ADD_TO_PLAYLIST_SONG";
    public static final String BUNDLE_KEY_PLAYLISTS = "PLAYLISTS";
    public static final String BUNDLE_KEY_IS_SONG = "IS_SONG";

    private DialogInterface.OnMultiChoiceClickListener onMultiChoiceClickListener;

    private DialogInterface.OnClickListener onClickListenerPositiveButton;

    private DialogInterface.OnClickListener onClickListenerNeutralButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        Bundle bundle = getArguments();
        if (builder != null && bundle != null) {
            @SuppressWarnings("unchecked")
            List<RandomPlaylist> randomPlaylists =
                    (List<RandomPlaylist>) bundle.getSerializable(BUNDLE_KEY_PLAYLISTS);
            List<Integer> selectedPlaylistIndices = new ArrayList<>();
            builder.setTitle(R.string.add_to_playlist);
            setUpChoices(builder, randomPlaylists, selectedPlaylistIndices);
            setUpButtons(builder, bundle, randomPlaylists, selectedPlaylistIndices);
            return builder.create();
        }
        return null;
    }

    private void setUpChoices(AlertDialog.Builder builder,
                              List<RandomPlaylist> randomPlaylists,
                              List<Integer> selectedPlaylistIndices) {
        onMultiChoiceClickListener =
                new OnMultiChoiceClickListenerAddToPlaylist(selectedPlaylistIndices);
        builder.setMultiChoiceItems(
                getPlaylistTitles(randomPlaylists), null, onMultiChoiceClickListener);
    }

    private String[] getPlaylistTitles(List<RandomPlaylist> randomPlaylists) {
        List<String> titles = new ArrayList<>(randomPlaylists.size());
        for (RandomPlaylist randomPlaylist : randomPlaylists) {
            titles.add(randomPlaylist.getName());
        }
        String[] titlesArray = new String[titles.size()];
        int i = 0;
        for (String title : titles) {
            titlesArray[i++] = title;
        }
        return titlesArray;
    }

    private void setUpButtons(AlertDialog.Builder builder, Bundle bundle,
                              List<RandomPlaylist> randomPlaylists,
                              List<Integer> selectedPlaylistIndices) {
        boolean isSong = bundle.getBoolean(BUNDLE_KEY_IS_SONG);
        AudioUri audioURI = (AudioUri) bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_SONG);
        RandomPlaylist randomPlaylist = (RandomPlaylist) bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST);
        onClickListenerPositiveButton = new OnClickListenerAddToPlaylistPositiveButton(
                randomPlaylists, selectedPlaylistIndices, isSong, audioURI, randomPlaylist);
        onClickListenerNeutralButton = new OnClickListenerAddToPlaylistNeutralButton(
                (ActivityMain) getActivity(), isSong, audioURI, randomPlaylist);
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

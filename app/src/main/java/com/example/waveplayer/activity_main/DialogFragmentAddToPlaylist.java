package com.example.waveplayer.activity_main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.random_playlist.RandomPlaylist;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        Bundle bundle = getArguments();
        if (builder != null && bundle != null) {
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
        onMultiChoiceClickListener =
                new OnMultiChoiceClickListenerAddToPlaylist(selectedPlaylistIndices);
        builder.setMultiChoiceItems(
                getPlaylistTitles(MediaData.getInstance().getPlaylists()),
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
        boolean isSong = bundle.getBoolean(BUNDLE_KEY_IS_SONG);
        Song song = (Song) bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_SONG);
        RandomPlaylist randomPlaylist = (RandomPlaylist) bundle.getSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST);
        onClickListenerPositiveButton = new OnClickListenerAddToPlaylistPositiveButton(
                selectedPlaylistIndices, isSong, song, randomPlaylist);
        onClickListenerNeutralButton = new OnClickListenerAddToPlaylistNeutralButton(
                (ActivityMain) requireActivity(), isSong, song, randomPlaylist);
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

package com.example.waveplayer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

public class DialogFragmentAddToPlaylist extends DialogFragment {

    ActivityMain activityMain;

    DialogFragmentAddToPlaylist(ActivityMain activityMain){
        this.activityMain = activityMain;
    }

    private DialogFragmentAddToPlaylist(){
        throw new UnsupportedOperationException();
    }

    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST = "ADD_TO_PLAYLIST_PLAYLIST";
    public static final String BUNDLE_KEY_ADD_TO_PLAYLIST_SONG = "ADD_TO_PLAYLIST_SONG";
    public static final String BUNDLE_KEY_PLAYLISTS = "PLAYLISTS";
    public static final String BUNDLE_KEY_IS_SONG = "IS_SONG";

    DialogInterface.OnMultiChoiceClickListener onMultiChoiceClickListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        final Bundle bundle = getArguments();
        final List<RandomPlaylist> randomPlaylists =
                (List<RandomPlaylist>) bundle.getSerializable(BUNDLE_KEY_PLAYLISTS);
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
        onMultiChoiceClickListener = new OnMultiChoiceClickListenerAddToPlaylist(selectedPlaylists);
        builder.setMultiChoiceItems(titlesArray,
                null, onMultiChoiceClickListener);
        builder.setTitle(R.string.add_to_playlist)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        boolean isSong = bundle.getBoolean(BUNDLE_KEY_IS_SONG);
                        AudioURI audioURI = (AudioURI) bundle.getSerializable(
                                BUNDLE_KEY_ADD_TO_PLAYLIST_SONG);
                        if(isSong && audioURI != null) {
                            for (int index : selectedPlaylists) {
                                randomPlaylists.get(index).getProbFun().add(audioURI);
                            }
                        }
                        RandomPlaylist randomPlaylist = (RandomPlaylist) bundle.getSerializable(
                                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST);
                        if(!isSong && randomPlaylist != null){
                            for(AudioURI audioURI1 : randomPlaylist.getProbFun().getProbMap().keySet()){
                                for (int index : selectedPlaylists) {
                                    randomPlaylists.get(index).getProbFun().add(audioURI1);
                                }
                            }
                        }
                    }
                }).setNeutralButton(R.string.new_playlist, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean isSong = bundle.getBoolean(BUNDLE_KEY_IS_SONG);
                activityMain.serviceMain.userPickedSongs.clear();
                activityMain.serviceMain.userPickedPlaylist = null;
                AudioURI audioURI = (AudioURI) bundle.getSerializable(
                        BUNDLE_KEY_ADD_TO_PLAYLIST_SONG);
                if(isSong && audioURI != null) {
                    activityMain.serviceMain.userPickedSongs.add(audioURI);
                }
                RandomPlaylist randomPlaylist = (RandomPlaylist) bundle.getSerializable(
                        BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST);
                if(!isSong && randomPlaylist != null){
                    activityMain.serviceMain.userPickedSongs.addAll(
                            randomPlaylist.getProbFun().getProbMap().keySet());
                }
                FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
                if (fragment != null) {
                    NavHostFragment.findNavController(fragment).navigate(R.id.fragmentEditPlaylist);
                }
            }
        });
        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onMultiChoiceClickListener = null;
    }
}

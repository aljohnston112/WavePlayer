package com.example.waveplayer.fragments.fragment_edit_playlist;

import android.view.View;
import android.widget.EditText;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

public class OnClickListenerFABFragmentEditPlaylist implements View.OnClickListener {

    private final FragmentEditPlaylist fragmentEditPlaylist;

    private final RandomPlaylist userPickedPlaylist;

    private final EditText editTextPlaylistName;

    private final List<AudioUri> userPickedSongs;

    OnClickListenerFABFragmentEditPlaylist(FragmentEditPlaylist fragmentEditPlaylist,
                                           RandomPlaylist userPickedPlaylist,
                                           EditText editTextPlaylistName,
                                           List<AudioUri> userPickedSongs) {
        this.userPickedPlaylist = userPickedPlaylist;
        this.editTextPlaylistName = editTextPlaylistName;
        this.fragmentEditPlaylist = fragmentEditPlaylist;
        this.userPickedSongs = userPickedSongs;
    }

    @Override
    public void onClick(View view) {
        ActivityMain activityMain = (ActivityMain) fragmentEditPlaylist.getActivity();
        int playlistIndex = indexOfPlaylistWName(editTextPlaylistName.getText().toString());
        if (userPickedSongs.size() == 0) {
            activityMain.showToast(R.string.not_enough_songs_for_playlist);
        } else if (editTextPlaylistName.length() == 0) {
            activityMain.showToast(R.string.no_name_playlist);
        } else if (playlistIndex != -1 && userPickedPlaylist == null) {
            activityMain.showToast(R.string.duplicate_name_playlist);
        } else if (userPickedPlaylist == null) {
            activityMain.addPlaylist(new RandomPlaylist(editTextPlaylistName.getText().toString(),
                    userPickedSongs, activityMain.getMaxPercent(), false, -1));
            for (AudioUri audioURI : userPickedSongs) {
                audioURI.setSelected(false);
            }
            userPickedSongs.clear();
            activityMain.saveFile();
            fragmentEditPlaylist.popBackStack();
            activityMain.hideKeyboard(view);
        } else {
            ArrayList<String> names = new ArrayList<>();
            for (RandomPlaylist randomPlaylist : activityMain.getPlaylists()) {
                names.add(randomPlaylist.getName());
            }
            if (userPickedPlaylist.getName().equals(
                    editTextPlaylistName.getText().toString()) || !names.contains(editTextPlaylistName.getText().toString())) {
                userPickedPlaylist.setName(editTextPlaylistName.getText().toString());
                for (AudioUri audioURI : userPickedPlaylist.getAudioUris()) {
                    if (!userPickedSongs.contains(audioURI)) {
                        userPickedPlaylist.remove(audioURI);
                    }
                }
                for (AudioUri audioURI : userPickedSongs) {
                    userPickedPlaylist.add(audioURI);
                    audioURI.setSelected(false);
                }
                activityMain.saveFile();
                fragmentEditPlaylist.popBackStack();
                activityMain.hideKeyboard(view);
            } else {
                activityMain.showToast(R.string.duplicate_name_playlist);
            }
        }
    }

    private int indexOfPlaylistWName(String playlistName) {
        ActivityMain activityMain = (ActivityMain) fragmentEditPlaylist.getActivity();
        int playlistIndex = -1;
        int i = 0;
        for (RandomPlaylist randomPlaylist : activityMain.getPlaylists()) {
            if (randomPlaylist.getName().equals(playlistName)) {
                playlistIndex = i;
            }
            i++;
        }
        return playlistIndex;
    }

}
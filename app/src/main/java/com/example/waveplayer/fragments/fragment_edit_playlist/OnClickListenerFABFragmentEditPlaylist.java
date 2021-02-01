package com.example.waveplayer.fragments.fragment_edit_playlist;

import android.view.View;
import android.widget.EditText;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

public class OnClickListenerFABFragmentEditPlaylist implements View.OnClickListener {

    private final FragmentEditPlaylist fragmentEditPlaylist;

    private final RandomPlaylist userPickedPlaylist;

    private final EditText editTextPlaylistName;

    private final List<Song> userPickedSongs;

    OnClickListenerFABFragmentEditPlaylist(FragmentEditPlaylist fragmentEditPlaylist,
                                           RandomPlaylist userPickedPlaylist,
                                           EditText editTextPlaylistName,
                                           List<Song> userPickedSongs) {
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
                    userPickedSongs, activityMain.getMaxPercent(), false));
            for (Song song : userPickedSongs) {
                song.setSelected(false);
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
                for (Song song : userPickedPlaylist.getSongs()) {
                    if (!userPickedSongs.contains(song)) {
                        userPickedPlaylist.remove(song);
                    }
                }
                for (Song song : userPickedSongs) {
                    userPickedPlaylist.add(song);
                    song.setSelected(false);
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
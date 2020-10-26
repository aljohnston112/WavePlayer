package com.example.waveplayer;

import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

public class OnClickListenerFABFragmentEditPlaylist implements View.OnClickListener {

    FragmentEditPlaylist fragmentEditPlaylist;

    final EditText playlistName;

    final ArrayList<AudioURI> finalPlaylistSongs;

    OnClickListenerFABFragmentEditPlaylist(FragmentEditPlaylist fragmentEditPlaylist,
                                           ArrayList<AudioURI> finalPlaylistSongs,
                                           EditText playlistName) {
        this.playlistName = playlistName;
        this.fragmentEditPlaylist = fragmentEditPlaylist;
        this.finalPlaylistSongs = finalPlaylistSongs;
    }

    @Override
    public void onClick(View view) {
        ActivityMain activityMain = fragmentEditPlaylist.activityMain;
        int playlistIndex = indexOfPlaylistWName(playlistName.getText().toString());
        if (activityMain.serviceMain.userPickedSongs.size() == 0) {
            Toast toast = Toast.makeText(activityMain.getApplicationContext(),
                    R.string.not_enough_songs_for_playlist, Toast.LENGTH_LONG);
            toast.show();
        } else if (playlistName.length() == 0) {
            Toast toast = Toast.makeText(activityMain.getApplicationContext(),
                    R.string.no_name_playlist, Toast.LENGTH_LONG);
            toast.show();
        } else if (playlistIndex != -1 &&
                activityMain.serviceMain.userPickedPlaylist == null) {
            Toast toast = Toast.makeText(activityMain.getApplicationContext(),
                    R.string.duplicate_name_playlist, Toast.LENGTH_LONG);
            toast.show();
        } else if (activityMain.serviceMain.userPickedPlaylist == null) {
            activityMain.serviceMain.playlists.add(new RandomPlaylist(
                    activityMain.serviceMain.userPickedSongs,
                    ServiceMain.MAX_PERCENT, playlistName.getText().toString(),
                    false, -1));
            for (AudioURI audioURI : activityMain.serviceMain.userPickedSongs) {
                audioURI.setSelected(false);
            }
            activityMain.serviceMain.userPickedSongs.clear();
            activityMain.serviceMain.saveFile();
            fragmentEditPlaylist.popBackStackAndHideKeyboard(view);
        } else {
            for (AudioURI audioURI : finalPlaylistSongs) {
                if (!activityMain.serviceMain.userPickedSongs.contains(audioURI)) {
                    activityMain.serviceMain.userPickedPlaylist.getProbFun().remove(audioURI);
                }
            }
            for (AudioURI audioURI : activityMain.serviceMain.userPickedSongs) {
                activityMain.serviceMain.userPickedPlaylist.getProbFun().add(audioURI);
                audioURI.setSelected(false);
            }
            activityMain.serviceMain.saveFile();
            fragmentEditPlaylist.popBackStackAndHideKeyboard(view);
        }
    }

    private int indexOfPlaylistWName(String playlistName) {
        ActivityMain activityMain = fragmentEditPlaylist.activityMain;
        int playlistIndex = -1;
        int i = 0;
        for (RandomPlaylist randomPlaylist : activityMain.serviceMain.playlists) {
            if (randomPlaylist.getName().equals(playlistName)) {
                playlistIndex = i;
            }
            i++;
        }
        return playlistIndex;
    }


}

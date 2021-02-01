package com.example.waveplayer.activity_main;

import android.content.DialogInterface;

import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.random_playlist.RandomPlaylist;

public class OnClickListenerAddToPlaylistNeutralButton implements DialogInterface.OnClickListener {

    private final ActivityMain activityMain;
    private final boolean isSong;
    private final Song song;
    private final RandomPlaylist randomPlaylist;

    private OnClickListenerAddToPlaylistNeutralButton() {
        throw new UnsupportedOperationException();
    }

    public OnClickListenerAddToPlaylistNeutralButton(
            ActivityMain activityMain,
            boolean isSong, Song song, RandomPlaylist randomPlaylist) {
        this.activityMain = activityMain;
        this.isSong = isSong;
        this.song = song;
        this.randomPlaylist = randomPlaylist;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
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
        }

}
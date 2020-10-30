package com.example.waveplayer;

import android.content.DialogInterface;

public class OnClickListenerAddToPlaylistNeutralButton implements DialogInterface.OnClickListener {

    final ActivityMain activityMain;
    final boolean isSong;
    final AudioUri audioURI;
    final RandomPlaylist randomPlaylist;

    private OnClickListenerAddToPlaylistNeutralButton() {
        throw new UnsupportedOperationException();
    }

    public OnClickListenerAddToPlaylistNeutralButton(
            ActivityMain activityMain,
            boolean isSong, AudioUri audioURI, RandomPlaylist randomPlaylist) {
        this.activityMain = activityMain;
        this.isSong = isSong;
        this.audioURI = audioURI;
        this.randomPlaylist = randomPlaylist;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
            // Needed for FragmentEditPlaylist to make a new playlist
            activityMain.setUserPickedPlaylist(null);
            activityMain.clearUserPickedSongs();
            if (isSong && audioURI != null) {
                activityMain.addUserPickedSong(audioURI);
            }
            if (!isSong && randomPlaylist != null) {
                for (AudioUri audioUriInPlaylist : randomPlaylist.getAudioUris()) {
                    activityMain.addUserPickedSong(audioUriInPlaylist);
                }
            }
            activityMain.navigateTo(R.id.fragmentEditPlaylist);
        }

}
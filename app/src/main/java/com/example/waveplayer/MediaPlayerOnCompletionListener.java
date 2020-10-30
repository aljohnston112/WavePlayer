package com.example.waveplayer;

import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

public class MediaPlayerOnCompletionListener implements MediaPlayer.OnCompletionListener {

    ServiceMain serviceMain;

    MediaPlayerOnCompletionListener(ServiceMain serviceMain) {
        this.serviceMain = serviceMain;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.v(ServiceMain.TAG, "Media player: " +
                mediaPlayer.hashCode() +
                " onCompletion started");
        if (serviceMain.getCurrentSong() != null) {
            if (serviceMain.loopingOne()) {
                serviceMain.seekTo(0);
                serviceMain.shouldStart(true);
                serviceMain.saveFile();
                return;
            }
        }
        if ((!serviceMain.shuffling() && !serviceMain.playlistHasNext() &&
                !serviceMain.looping())) {
            serviceMain.shutDownSeekBarUpdater();
        }
        serviceMain.stopAndPreparePrevious();
        if (!serviceMain.shuffling()) {
            serviceMain.playNextInPlaylist();
        } else {
            serviceMain.playNextInQueue(true);
        }

        sendBroadcastOnCompletion();
        Log.v(ServiceMain.TAG, "Media player: " +
                mediaPlayer.hashCode() +
                " onCompletion ended");
    }

    private void sendBroadcastOnCompletion() {
        Log.v(ServiceMain.TAG, "onCompletion broadcast started");
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(serviceMain.getResources().getString(R.string.broadcast_receiver_action_on_completion));
        serviceMain.sendBroadcast(intent);
        Log.v(ServiceMain.TAG, "onCompletion broadcast ended");
    }

}

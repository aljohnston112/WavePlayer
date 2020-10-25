package com.example.waveplayer;

import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.concurrent.Executors;

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
        if (serviceMain.currentSong != null) {
            if (serviceMain.loopingOne) {
                MediaPlayerWURI mediaPlayerWURI = serviceMain.uriMediaPlayerWURIHashMap.get(
                        serviceMain.currentSong.getUri());
                if (mediaPlayerWURI != null) {
                    mediaPlayerWURI.seekTo(0);
                    mediaPlayerWURI.shouldStart(true);
                    serviceMain.addToQueueAtCurrentIndex(mediaPlayerWURI.audioURI.getUri());
                }
                serviceMain.saveFile();
                return;
            }
        }
        if ((!serviceMain.shuffling && !serviceMain.currentPlaylistIterator.hasNext() &&
                !serviceMain.looping)) {
            serviceMain.scheduledExecutorService.shutdown();
            if (serviceMain.fragmentSongVisible) {
                serviceMain.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            }
        }
        serviceMain.stopAndPreparePrevious();
        if (!serviceMain.shuffling) {
            if (serviceMain.currentPlaylistIterator.hasNext()) {
                serviceMain.addToQueueAndPlay(serviceMain.currentPlaylistIterator.next().getUri());
            } else if (serviceMain.looping) {
                serviceMain.currentPlaylistIterator =
                        serviceMain.currentPlaylistArray.listIterator(0);
                serviceMain.addToQueueAndPlay(serviceMain.currentPlaylistIterator.next().getUri());
            } else {
                serviceMain.isPlaying = false;
                serviceMain.songInProgress = false;
            }
        } else {
            if (serviceMain.songQueueIterator.hasNext()) {
                serviceMain.playNextInQueue();
            } else if (serviceMain.looping) {
                serviceMain.songQueueIterator = serviceMain.songQueue.listIterator(0);
                serviceMain.play(serviceMain.songQueueIterator.next());
            } else {
                serviceMain.addToQueueAndPlay(
                        serviceMain.currentPlaylist.getProbFun().fun(ServiceMain.random));
            }
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

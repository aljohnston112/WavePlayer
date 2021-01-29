package com.example.waveplayer.media_controller;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.waveplayer.random_playlist.AudioUri;

import java.util.concurrent.Callable;

public class CallableCreateMediaPlayerWUri implements Callable<MediaPlayerWUri> {

    final Context context;

    final MediaController mediaController;

    final MediaPlayerOnCompletionListener mediaPlayerOnCompletionListener;

    final Long id;

    CallableCreateMediaPlayerWUri(Context context, MediaController mediaController,
                                  MediaPlayerOnCompletionListener mediaPlayerOnCompletionListener,
                                  Long songID) {
        this.context = context;
        this.id = songID;
        this.mediaController = mediaController;
        this.mediaPlayerOnCompletionListener = mediaPlayerOnCompletionListener;
    }

    @Override
    public MediaPlayerWUri call() {
        MediaPlayerWUri mediaPlayerWURI = new MediaPlayerWUri(context,
                mediaController, MediaPlayer.create(
                context, AudioUri.getUri(id)), id);
        mediaPlayerWURI.setOnCompletionListener(mediaPlayerOnCompletionListener);
        return mediaPlayerWURI;
    }

}

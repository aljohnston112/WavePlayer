package com.example.waveplayer;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.waveplayer.random_playlist.AudioUri;

import java.util.concurrent.Callable;

public class CallableCreateMediaPlayerWURI implements Callable<MediaPlayerWUri> {


    final Context context;

    final MediaController mediaController;

    final Long id;

    CallableCreateMediaPlayerWURI(Context context, MediaController mediaController, Long songID) {
        this.context = context;
        this.id = songID;
        this.mediaController = mediaController;
    }

    @Override
    public MediaPlayerWUri call() {
        MediaPlayerWUri mediaPlayerWURI = new MediaPlayerWUri(
                mediaController, MediaPlayer.create(
                context, AudioUri.getUri(id)), id);
        mediaPlayerWURI.setOnCompletionListener(mediaController.onCompletionListener);
        return mediaPlayerWURI;
    }

}

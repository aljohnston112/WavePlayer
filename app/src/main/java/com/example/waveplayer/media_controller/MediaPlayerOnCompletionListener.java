package com.example.waveplayer.media_controller;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.MediaController;

public class MediaPlayerOnCompletionListener implements MediaPlayer.OnCompletionListener {

    private final Context context;

    private final MediaController mediaController;

    MediaPlayerOnCompletionListener(Context context, MediaController mediaController) {
        this.mediaController = mediaController;
        this.context = context;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaController.playNext(context);
        sendBroadcastOnCompletion();
    }

    private void sendBroadcastOnCompletion() {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(context.getResources().getString(R.string.broadcast_receiver_action_on_completion));
        context.sendBroadcast(intent);
    }

}
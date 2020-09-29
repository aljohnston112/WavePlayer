package com.example.waveplayer;

import android.media.MediaPlayer;

public class MediaPlayerWURI {

    final MediaPlayer mediaPlayer;

    final AudioURI audioURI;

    MediaPlayerWURI(MediaPlayer mediaPlayer, AudioURI audioURI){
        this.mediaPlayer = mediaPlayer;
        this.audioURI = audioURI;
    }

}

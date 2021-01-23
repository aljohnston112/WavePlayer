package com.example.waveplayer.service_main;

import android.widget.SeekBar;
import android.widget.TextView;

import com.example.waveplayer.media_controller.MediaPlayerWUri;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RunnableSeekBarUpdater implements Runnable {

    private final MediaPlayerWUri mediaPlayerWURI;

    private final SeekBar seekBar;

    private final TextView textViewCurrentTime;

    private final Locale locale;

    Runnable runnableSeekBarUpdater = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayerWURI.isPlaying()) {
                int currentMilliseconds = mediaPlayerWURI.getCurrentPosition();
                seekBar.setProgress(currentMilliseconds);
                final String currentTime = String.format(locale,
                        "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(currentMilliseconds),
                        TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(currentMilliseconds)),
                        TimeUnit.MILLISECONDS.toSeconds(currentMilliseconds) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds)));
                textViewCurrentTime.setText(currentTime);
            }
        }
    };

    public RunnableSeekBarUpdater(
            MediaPlayerWUri mediaPlayerWURI, SeekBar seekBar, TextView textViewCurrentTime, Locale locale) {
        this.mediaPlayerWURI = mediaPlayerWURI;
        this.seekBar = seekBar;
        this.textViewCurrentTime = textViewCurrentTime;
        this.locale = locale;
    }

    @Override
    public void run() {
        seekBar.post(runnableSeekBarUpdater);
    }

    public void shutDown(){
        runnableSeekBarUpdater = null;
    }

}
package com.example.waveplayer;

import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RunnableSeekBarUpdater implements Runnable {

    final MediaPlayerWURI mediaPlayerWURI;

    final SeekBar seekBar;

    final int milliSeconds;

    final TextView textViewCurrentTime;

    final Locale locale;

    public RunnableSeekBarUpdater(
            MediaPlayerWURI mediaPlayerWURI, SeekBar seekBar, TextView textViewCurrentTime, int milliSeconds, Locale locale) {
        this.mediaPlayerWURI = mediaPlayerWURI;
        this.seekBar = seekBar;
        this.textViewCurrentTime = textViewCurrentTime;
        this.milliSeconds = milliSeconds;
        this.locale = locale;
    }

    @Override
    public void run() {
        seekBar.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayerWURI.isPlaying()) {
                    int currentMilliseconds = mediaPlayerWURI.getCurrentPosition();
                    seekBar.setProgress(currentMilliseconds+2);
                    final String currentTime = String.format(locale,
                            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(currentMilliseconds),
                            TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds) -
                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(currentMilliseconds)),
                            TimeUnit.MILLISECONDS.toSeconds(currentMilliseconds) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds)));
                    textViewCurrentTime.setText(currentTime);
                }
            }
        });
    }
}

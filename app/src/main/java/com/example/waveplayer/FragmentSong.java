package com.example.waveplayer;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FragmentSong extends Fragment {

    ActivityMain activityMain;

    public FragmentSong() {
        // Required empty public constructor
    }

    public static FragmentSong newInstance() {
        return new FragmentSong();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        activityMain.actionBar.setTitle(R.string.now_playing);
        activityMain.fab.hide();

        setUpButtons(view);

        TextView textViewSongName = view.findViewById(R.id.text_view_song_name);
        textViewSongName.setText(activityMain.currentSong.title);

        int millis = activityMain.currentSong.duration;

        SeekBar seekBar = view.findViewById(R.id.seekBar);
        seekBar.setMax(millis);

        String endTime = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));

        TextView textViewCurrent = view.findViewById(R.id.editTextCurrentTime);
        textViewCurrent.setText("00:00:00");

        TextView textViewEnd = view.findViewById(R.id.editTextEndTime);
        textViewEnd.setText(endTime);
        playSelectedWaveFile(activityMain.currentSong);
    }

    private void setUpButtons(View view) {

        ImageButton buttonBad = activityMain.findViewById(R.id.button_thumb_down);

    }

    private void playSelectedWaveFile(AudioURI uri) {
        AudioManager audioManager = (AudioManager) activityMain.getSystemService(Context.AUDIO_SERVICE);
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build());
        } else {
            result = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int i) {

                }
            }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                activityMain.play(uri);
        }
    }

}
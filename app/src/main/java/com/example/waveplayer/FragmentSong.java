package com.example.waveplayer;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

import java.util.concurrent.TimeUnit;

public class FragmentSong extends Fragment {

    ActivityMain activityMain;

    SeekBar seekBar;

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
        activityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
        activityMain.showFab(false);
        setUpButtons(view);
        playSelectedWaveFile(activityMain.currentSong);
    }

    private void setUpButtons(View view) {
        ImageButton buttonBad = activityMain.findViewById(R.id.button_thumb_down);
        ImageButton buttonGood = activityMain.findViewById(R.id.button_thumb_up);
        ImageButton buttonShuffle = activityMain.findViewById(R.id.imageButtonShuffle);
        ImageButton buttonPrev = activityMain.findViewById(R.id.imageButtonPrev);
        ImageButton buttonPause = activityMain.findViewById(R.id.imageButtonPause);
        ImageButton buttonNext = activityMain.findViewById(R.id.imageButtonNext);
        ImageButton buttonLoop = activityMain.findViewById(R.id.imageButtonRepeat);

        buttonBad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.currentPlaylist.getProbFun().bad(
                        activityMain.currentSong, ActivityMain.PERCENT_CHANGE);
            }
        });

        buttonGood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.currentPlaylist.getProbFun().good(
                        activityMain.currentSong, ActivityMain.PERCENT_CHANGE);
            }
        });

        buttonShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               //TODO
            }
        });

        buttonPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.playPrevious();
            }
        });

        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.pauseOrPlay();
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.currentPlaylist.getProbFun().bad(activityMain.currentSong, ActivityMain.PERCENT_CHANGE);
                activityMain.playNext();
            }
        });

        buttonLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO
            }
        });

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
package com.example.waveplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.Fragment;

public class FragmentSong extends Fragment {

    ActivityMain activityMain;

    SeekBar seekBar;

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
        if (activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
            activityMain.showFab(false);
            activityMain.updateSongUI();
            if(activityMain.serviceMain!=null) {
                activityMain.serviceMain.updateNotification();
            }
            setUpButtons(view);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpButtons(View view) {
        final ImageButton buttonBad = activityMain.findViewById(R.id.button_thumb_down);
        final ImageButton buttonGood = activityMain.findViewById(R.id.button_thumb_up);
        final ImageButton buttonShuffle = activityMain.findViewById(R.id.imageButtonShuffle);
        final  ImageButton buttonPrev = activityMain.findViewById(R.id.imageButtonPrev);
        final ImageButton buttonPause = activityMain.findViewById(R.id.imageButtonPlayPause);
        final ImageButton buttonNext = activityMain.findViewById(R.id.imageButtonNext);
        final  ImageButton buttonLoop = activityMain.findViewById(R.id.imageButtonRepeat);

        buttonBad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.serviceMain.currentPlaylist.getProbFun().bad(
                        activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
            }
        });

        buttonBad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonBad.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                    case MotionEvent.ACTION_UP:
                        buttonBad.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return false;
            }
        });


        buttonGood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.serviceMain.currentPlaylist.getProbFun().good(
                        activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
            }
        });

        buttonGood.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonGood.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                    case MotionEvent.ACTION_UP:
                        buttonGood.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return false;
            }
        });

        buttonShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO
            }
        });

        buttonShuffle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonShuffle.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                    case MotionEvent.ACTION_UP:
                        buttonShuffle.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return false;
            }
        });

        buttonPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.playPrevious();
            }
        });

        buttonPrev.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonPrev.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                    case MotionEvent.ACTION_UP:
                        buttonPrev.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return false;
            }
        });

        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                activityMain.pauseOrPlay();
            }
        });

        buttonPause.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonPause.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                    case MotionEvent.ACTION_UP:
                        buttonPause.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return false;
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.playNext();
            }
        });

        buttonNext.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonNext.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                    case MotionEvent.ACTION_UP:
                        buttonNext.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return false;
            }
        });

        buttonLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO
            }
        });

        buttonLoop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonLoop.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                    case MotionEvent.ACTION_UP:
                        buttonLoop.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                }
                return false;
            }
        });

    }

}
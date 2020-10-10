package com.example.waveplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    View view;

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
        this.view = view;
        activityMain = ((ActivityMain) getActivity());
        if (activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
            activityMain.showFab(false);
            if(activityMain.serviceMain!=null) {
                activityMain.updateSongUI();
                activityMain.serviceMain.updateNotification();
                setUpButtons(view);
            }
        }
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction("ServiceConnected");
        activityMain.registerReceiver(new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyServiceConnected();
            }
        }, filterComplete);
    }

    public void notifyServiceConnected(){
        activityMain.updateSongUI();
        activityMain.serviceMain.updateNotification();
        setUpButtons(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpButtons(View view) {
        final ImageButton buttonBad = view.findViewById(R.id.button_thumb_down);
        final ImageButton buttonGood = view.findViewById(R.id.button_thumb_up);
        final ImageButton buttonShuffle = view.findViewById(R.id.imageButtonShuffle);
        final  ImageButton buttonPrev = view.findViewById(R.id.imageButtonPrev);
        final ImageButton buttonPause = view.findViewById(R.id.imageButtonPlayPause);
        final ImageButton buttonNext = view.findViewById(R.id.imageButtonNext);
        final  ImageButton buttonLoop = view.findViewById(R.id.imageButtonRepeat);

        buttonBad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.serviceMain.currentPlaylist.getProbFun().bad(
                        activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
                activityMain.serviceMain.saveFile();
            }
        });

        buttonBad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonBad.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                        return true;
                    case MotionEvent.ACTION_UP:
                        buttonBad.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        buttonBad.performClick();
                        return true;
                }
                return false;
            }
        });


        buttonGood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.serviceMain.currentPlaylist.getProbFun().good(
                        activityMain.serviceMain.getCurrentSong(), ServiceMain.PERCENT_CHANGE);
                activityMain.serviceMain.saveFile();
            }
        });

        buttonGood.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        buttonGood.setBackgroundColor(getResources().getColor(R.color.colorOnSecondary));
                        return true;
                    case MotionEvent.ACTION_UP:
                        buttonGood.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        buttonGood.performClick();
                        return true;
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
                        return true;
                    case MotionEvent.ACTION_UP:
                        buttonShuffle.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        buttonShuffle.performClick();
                        return true;
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
                        return true;
                    case MotionEvent.ACTION_UP:
                        buttonPrev.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        buttonPrev.performClick();
                        return true;
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
                        return true;
                    case MotionEvent.ACTION_UP:
                        buttonPause.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        buttonPause.performClick();
                        return true;
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
                        return true;
                    case MotionEvent.ACTION_UP:
                        buttonNext.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        buttonNext.performClick();
                        return true;
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
                        return true;
                    case MotionEvent.ACTION_UP:
                        buttonLoop.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                        buttonLoop.performClick();
                        return true;
                }
                return false;
            }
        });

    }

}
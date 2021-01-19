package com.example.waveplayer.fragments.fragment_song;

import android.view.MotionEvent;
import android.view.View;

import com.example.waveplayer.R;

public class OnTouchListenerFragmentSongButtons implements View.OnTouchListener {

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                view.setBackgroundColor(view.getResources().getColor(R.color.colorOnSecondary));
                return true;
            case MotionEvent.ACTION_UP:
                view.setBackgroundColor(view.getResources().getColor(R.color.colorPrimary));
                view.performClick();
                return true;
        }
        return false;
    }

}

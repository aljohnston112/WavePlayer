package com.example.waveplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import static com.example.waveplayer.FragmentSongPane.getResizedBitmap;

public class FragmentSong extends Fragment {

    ActivityMain activityMain;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

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
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        if(activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
            activityMain.showFab(false);
            activityMain.updateSongUI();
        }
        setUpButtons(view);
        setUpBroadcastReceiver(view);
    }

    private void setUpBroadcastReceiver(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyServiceConnected(view);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    public void notifyServiceConnected(View view) {
        activityMain.updateSongUI();
        setUpButtons(view);

    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpButtons(final View view) {
        final ImageButton buttonBad = view.findViewById(R.id.button_thumb_down);
        final ImageButton buttonGood = view.findViewById(R.id.button_thumb_up);
        final ImageButton buttonShuffle = view.findViewById(R.id.imageButtonShuffle);
        final ImageButton buttonPrev = view.findViewById(R.id.imageButtonPrev);
        final ImageButton buttonPause = view.findViewById(R.id.imageButtonPlayPause);
        final ImageButton buttonNext = view.findViewById(R.id.imageButtonNext);
        final ImageButton buttonLoop = view.findViewById(R.id.imageButtonRepeat);
        OnClickListenerFragmentSong onClickListenerFragmentSong = new OnClickListenerFragmentSong(activityMain);
        buttonBad.setOnClickListener(onClickListenerFragmentSong);
        buttonGood.setOnClickListener(onClickListenerFragmentSong);
        buttonShuffle.setOnClickListener(onClickListenerFragmentSong);
        buttonPrev.setOnClickListener(onClickListenerFragmentSong);
        buttonPause.setOnClickListener(onClickListenerFragmentSong);
        buttonNext.setOnClickListener(onClickListenerFragmentSong);
        buttonLoop.setOnClickListener(onClickListenerFragmentSong);
        buttonBad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
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
        buttonGood.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
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
        buttonShuffle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
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
        buttonPrev.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
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
        buttonPause.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
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
        buttonNext.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
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
        buttonLoop.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
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

        if(activityMain.serviceMain != null) {
            if (activityMain.serviceMain.shuffling) {
                buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp);
            } else {
                buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp);
            }

            if (activityMain.serviceMain.loopingOne) {
                buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp);
            } else if (activityMain.serviceMain.looping) {
                buttonLoop.setImageResource(R.drawable.repeat_black_24dp);
            } else {
                buttonLoop.setImageResource(R.drawable.repeat_white_24dp);
            }
        }

        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                // TODO
                //setUpGood();
                //setUpBad();
                //setUpShuffle();
                setUpPrev();
                setUpPlay();
                setUpNext();
                //setUpLoop();
            }

            private void setUpNext() {
                ImageView imageView = view.findViewById(R.id.imageButtonNext);
                int width = imageView.getMeasuredWidth();
                //noinspection SuspiciousNameCombination
                int height = width;
                Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.skip_next_black_24dp, null);
                if (drawable != null) {
                    drawable.setBounds(0, 0, width, height);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.draw(canvas);
                    Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
                    bitmap.recycle();
                    imageView.setImageBitmap(bitmapResized);
                }
            }

            private void setUpPlay() {
                ImageView imageView = view.findViewById(R.id.imageButtonPlayPause);
                int width = imageView.getMeasuredWidth();
                //noinspection SuspiciousNameCombination
                int height = width;
                Drawable drawable;
                ActivityMain activityMain = ((ActivityMain) getActivity());
                if (activityMain != null && activityMain.serviceMain != null && activityMain.serviceMain.isPlaying()) {
                    drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.pause_black_24dp, null);
                } else {
                    drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.play_arrow_black_24dp, null);
                }
                if (drawable != null) {
                    drawable.setBounds(0, 0, width, height);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.draw(canvas);
                    Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
                    bitmap.recycle();
                    imageView.setImageBitmap(bitmapResized);
                }
            }

            private void setUpPrev() {
                ImageView imageView = view.findViewById(R.id.imageButtonPrev);
                int width = imageView.getMeasuredWidth();
                //noinspection SuspiciousNameCombination
                int height = width;
                Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.skip_previous_black_24dp, null);
                if (drawable != null) {
                    drawable.setBounds(0, 0, width, height);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.draw(canvas);
                    Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
                    bitmap.recycle();
                    imageView.setImageBitmap(bitmapResized);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain = null;
    }

}
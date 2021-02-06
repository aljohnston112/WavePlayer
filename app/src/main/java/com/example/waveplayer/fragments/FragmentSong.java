package com.example.waveplayer.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentSongBinding;
import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.BitmapLoader;
import com.example.waveplayer.media_controller.MediaPlayerWUri;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.Song;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FragmentSong extends Fragment {

    private FragmentSongBinding binding;

    private ViewModelActivityMain viewModelActivityMain;

    private BroadcastReceiver broadcastReceiver;

    private SeekBar.OnSeekBarChangeListener onSeekBarChangeListener;

    private View.OnClickListener onClickListenerFragmentSong;
    private View.OnLongClickListener onLongClickListener;
    private View.OnTouchListener onTouchListenerFragmentSongButtons;
    private View.OnLayoutChangeListener onLayoutChangeListenerFragmentSongButtons;

    // For updating the SeekBar
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private Runnable runnableSeekBarUpdater;
    private Runnable runnableSongArtUpdater;

    private Observer<AudioUri> observerCurrentSong;
    private Observer<Boolean> observerIsPlaying;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        binding = FragmentSongBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(view);
        viewModelActivityMain.setSongToAddToQueue(activityMain.getCurrentSong().id);
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
        viewModelActivityMain.showFab(false);
        onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ActivityMain activityMain = (ActivityMain) requireActivity();
                activityMain.seekTo(seekBar.getProgress());
            }
        };
        runnableSongArtUpdater = () -> {
            ImageView imageViewSongArt = binding.imageViewSongArt;
            int songArtHeight = imageViewSongArt.getHeight();
            int songArtWidth = imageViewSongArt.getWidth();
            if (songArtWidth > songArtHeight) {
                songArtWidth = songArtHeight;
            } else {
                songArtHeight = songArtWidth;
            }
            if (songArtHeight > 0 && songArtWidth > 0) {
                Bitmap bitmap = BitmapLoader.getThumbnail(activityMain.getCurrentUri(),
                        songArtWidth, songArtHeight, activityMain.getApplicationContext());
                if (bitmap == null) {
                    Drawable drawable = ResourcesCompat.getDrawable(imageViewSongArt.getResources(),
                            R.drawable.music_note_black_48dp, null);
                    if (drawable != null) {
                        drawable.setBounds(0, 0, songArtWidth, songArtHeight);
                        Bitmap bitmapDrawable = Bitmap.createBitmap(songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmapDrawable);
                        Paint paint = new Paint();
                        paint.setColor(imageViewSongArt.getResources().getColor(R.color.colorPrimary));
                        canvas.drawRect(0, 0, songArtWidth, songArtHeight, paint);
                        drawable.draw(canvas);
                        imageViewSongArt.setImageBitmap(bitmapDrawable);
                    }
                } else {
                    imageViewSongArt.setImageBitmap(bitmap);
                }
            }
        };
        observerCurrentSong = s -> {
            updateSongUI();
        };
        viewModelActivityMain.getCurrentSong().observe(getViewLifecycleOwner(), observerCurrentSong);
        observerIsPlaying = b -> {
            updateSongPlayButton();
        };
        viewModelActivityMain.isPlaying().observe(getViewLifecycleOwner(), observerIsPlaying);
        setUpButtons();
        setUpBroadcastReceiver();
    }

    private void updateSongUI() {
        // Log.v(TAG, "updateSongUI start");
        // Log.v(TAG, "updating SongUI");
        updateSongArt();
        updateSongName();
        updateTextViewTimes();
        updateSongPlayButton();
        updateSeekBar();
        // Log.v(TAG, "updateSongUI end");
    }

    private void updateSongArt() {
        // Log.v(TAG, "updateSongArt start");
        final ImageView imageViewSongArt = binding.imageViewSongArt;
            imageViewSongArt.post(runnableSongArtUpdater);
        // Log.v(TAG, "updateSongArt end");
    }

    private void updateSongName() {
        // Log.v(TAG, "updateSongName start");
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        TextView textViewSongName = binding.textViewSongName;
        textViewSongName.setText(activityMain.getCurrentAudioUri().title);
        // Log.v(TAG, "updateSongName end");
    }

    private void updateTextViewTimes() {
        // Log.v(TAG, "updateTextViewTimes start");
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        final int maxMillis =
                activityMain.getCurrentAudioUri().getDuration(activityMain.getApplicationContext());
        final String stringEndTime = formatMillis(maxMillis);
        String stringCurrentTime = formatMillis(activityMain.getCurrentTime());
        TextView textViewCurrent = binding.editTextCurrentTime;
            textViewCurrent.setText(stringCurrentTime);
        TextView textViewEnd = binding.editTextEndTime;
            textViewEnd.setText(stringEndTime);
        // Log.v(TAG, "updateTextViewTimes end");
    }

    private String formatMillis(int millis) {
        // Log.v(TAG, "formatMillis start and end");
        return String.format(getResources().getConfiguration().locale,
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    private void updateSongPlayButton() {
        // Log.v(TAG, "updateSongPlayButton start");
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageButton imageButtonPlayPause = binding.imageButtonPlayPause;
        // Log.v(TAG, "updating SongPlayButton");
        if (activityMain.isPlaying()) {
            imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                    getResources(), R.drawable.pause_black_24dp, null));
        } else {
            imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                    getResources(), R.drawable.play_arrow_black_24dp, null));
        }
        // Log.v(TAG, "updateSongPlayButton end");
    }

    private void updateSeekBar() {
        // Log.v(TAG, "updateSeekBar start");
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        SeekBar seekBar = binding.seekBar;
        AudioUri audioUri = activityMain.getCurrentAudioUri();
        final int maxMillis;
        if (audioUri != null) {
            maxMillis = audioUri.getDuration(activityMain.getApplicationContext());
        } else {
            maxMillis = 9999;
        }
        seekBar.setMax(maxMillis);
        seekBar.setProgress(activityMain.getCurrentTime());
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        setUpSeekBarUpdater();
        // Log.v(TAG, "updateSeekBar end");
    }

    private void setUpSeekBarUpdater() {
        // Log.v(TAG, "setUpSeekBarUpdater start");
        SeekBar seekBar = binding.seekBar;
        TextView textViewCurrent = binding.editTextCurrentTime;
        ActivityMain activityMain = (ActivityMain) requireActivity();
        shutDownSeekBarUpdater();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        MediaPlayerWUri mediaPlayerWUri = activityMain.getCurrentMediaPlayerWUri();
        if (mediaPlayerWUri != null) {
            runnableSeekBarUpdater = () -> seekBar.post(() -> {
                if (mediaPlayerWUri.isPrepared()) {
                    int currentMilliseconds = mediaPlayerWUri.getCurrentPosition();
                    if(seekBar.getProgress() != currentMilliseconds) {
                        seekBar.setProgress(currentMilliseconds);
                        final String currentTime = formatMillis(currentMilliseconds);
                        textViewCurrent.setText(currentTime);
                    }
                }
            });
            scheduledExecutorService.scheduleAtFixedRate(
                    runnableSeekBarUpdater, 0L, 1L, TimeUnit.SECONDS);
        }
        // Log.v(TAG, "setUpSeekBarUpdater end");
    }

    public void shutDownSeekBarUpdater() {
        // Log.v(TAG, "shutDownSeekBarUpdater start");
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
        runnableSeekBarUpdater = null;
        // Log.v(TAG, "shutDownSeekBarUpdater end");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpButtons() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        final ImageButton buttonBad = binding.buttonThumbDown;
        final ImageButton buttonGood = binding.buttonThumbUp;
        final ImageButton buttonShuffle = binding.imageButtonShuffle;
        final ImageButton buttonPrev = binding.imageButtonPrev;
        final ImageButton buttonPause = binding.imageButtonPlayPause;
        final ImageButton buttonNext = binding.imageButtonNext;
        final ImageButton buttonLoop = binding.imageButtonRepeat;
        onClickListenerFragmentSong = clickedView -> {
            synchronized (ActivityMain.MUSIC_CONTROL_LOCK) {
                if (clickedView.getId() == R.id.button_thumb_down) {
                    Song song = activityMain.getSong(activityMain.getCurrentAudioUri().id);
                    if (song != null) {
                        activityMain.getCurrentPlaylist().globalBad(
                                song, activityMain.getPercentChangeDown());
                        activityMain.saveFile();
                    }
                } else if (clickedView.getId() == R.id.button_thumb_up) {
                    Song song = activityMain.getSong(activityMain.getCurrentAudioUri().id);
                    if (song != null) {
                        activityMain.getCurrentPlaylist().globalGood(
                                song, activityMain.getPercentChangeUp());
                        activityMain.saveFile();
                    }
                } else if (clickedView.getId() == R.id.imageButtonShuffle) {
                    ImageButton imageButton = (ImageButton) clickedView;
                    if (activityMain.isShuffling()) {
                        activityMain.setShuffling(false);
                        imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp);
                    } else {
                        activityMain.setShuffling(true);
                        imageButton.setImageResource(R.drawable.ic_shuffle_black_24dp);
                    }
                } else if (clickedView.getId() == R.id.imageButtonPrev) {
                    activityMain.playPrevious();
                } else if (clickedView.getId() == R.id.imageButtonPlayPause) {
                    activityMain.pauseOrPlay();
                } else if (clickedView.getId() == R.id.imageButtonNext) {
                    activityMain.playNext();
                } else if (clickedView.getId() == R.id.imageButtonRepeat) {
                    ImageButton imageButton = (ImageButton) clickedView;
                    if (activityMain.isLoopingOne()) {
                        activityMain.setLoopingOne(false);
                        imageButton.setImageResource(R.drawable.repeat_white_24dp);
                    } else if (activityMain.isLooping()) {
                        activityMain.setLooping(false);
                        activityMain.setLoopingOne(true);
                        imageButton.setImageResource(R.drawable.repeat_one_black_24dp);
                    } else {
                        activityMain.setLooping(true);
                        activityMain.setLoopingOne(false);
                        imageButton.setImageResource(R.drawable.repeat_black_24dp);
                    }
                }
            }
        };
        buttonBad.setOnClickListener(onClickListenerFragmentSong);
        buttonGood.setOnClickListener(onClickListenerFragmentSong);
        buttonShuffle.setOnClickListener(onClickListenerFragmentSong);
        buttonPrev.setOnClickListener(onClickListenerFragmentSong);
        buttonPause.setOnClickListener(onClickListenerFragmentSong);
        buttonNext.setOnClickListener(onClickListenerFragmentSong);
        buttonLoop.setOnClickListener(onClickListenerFragmentSong);
        buttonNext.setLongClickable(true);
        onLongClickListener = v -> {
            // TODO change color of button
            activityMain.getCurrentPlaylist().globalBad(
                    activityMain.getSong(activityMain.getCurrentAudioUri().id),
                    activityMain.getPercentChangeDown());
            return true;

        };
        buttonNext.setOnLongClickListener(onLongClickListener);
        onTouchListenerFragmentSongButtons = (view1, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view1.setBackgroundColor(view1.getResources().getColor(R.color.colorOnSecondary));
                    return false;
                case MotionEvent.ACTION_UP:
                    view1.setBackgroundColor(view1.getResources().getColor(R.color.colorPrimary));
                    view1.performClick();
                    return true;
            }
            return false;
        };
        buttonBad.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonGood.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonShuffle.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonPrev.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonPause.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonNext.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonLoop.setOnTouchListener(onTouchListenerFragmentSongButtons);
        if (activityMain.isShuffling()) {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp);
        } else {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp);
        }
        if (activityMain.isLoopingOne()) {
            buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp);
        } else if (activityMain.isLooping()) {
            buttonLoop.setImageResource(R.drawable.repeat_black_24dp);
        } else {
            buttonLoop.setImageResource(R.drawable.repeat_white_24dp);
        }
        onLayoutChangeListenerFragmentSongButtons =
                (view12, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    if(activityMain.fragmentSongVisible()) {
                        setUpButton(binding.buttonThumbUp, R.drawable.thumb_up_alt_black_24dp);
                        setUpButton(binding.buttonThumbDown, R.drawable.thumb_down_alt_black_24dp);
                        setUpButton(binding.imageButtonPrev, R.drawable.skip_previous_black_24dp);
                        setUpButton(binding.imageButtonNext, R.drawable.skip_next_black_24dp);
                        setUpShuffle();
                        setUpPlay();
                        setUpLoop();
                    }
                };
        view.addOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
    }

    private void setUpButton(ImageView imageView, int drawableID){
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(
                imageView.getResources(), drawableID, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            imageView.setImageBitmap(bitmap);
        }
    }

    private void setUpShuffle() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = binding.imageButtonShuffle;
        if (activityMain.isShuffling()) {
            setUpButton(imageView, R.drawable.ic_shuffle_black_24dp);
        } else {
            setUpButton(imageView, R.drawable.ic_shuffle_white_24dp);
        }
    }

    private void setUpPlay() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = binding.imageButtonPlayPause;
        if (activityMain.isPlaying()) {
            setUpButton(imageView, R.drawable.pause_black_24dp);
        } else {
            setUpButton(imageView, R.drawable.play_arrow_black_24dp);
        }
    }

    private void setUpLoop() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = binding.imageButtonRepeat;
        if (activityMain.isLoopingOne()) {
            setUpButton(imageView, R.drawable.repeat_one_black_24dp);
        } else if (activityMain.isLooping()) {
            setUpButton(imageView, R.drawable.repeat_black_24dp);
        } else {
            setUpButton(imageView, R.drawable.repeat_white_24dp);
        }
    }

    private void setUpBroadcastReceiver() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu));
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_service_connected))) {
                        setUpButtons();
                        updateSongUI();
                    }
                    if (action.equals(getResources().getString(
                            R.string.broadcast_receiver_action_on_create_options_menu))) {
                        setUpToolbar();
                    }
                }
            }
        };
        activityMain.registerReceiver(broadcastReceiver, filterComplete);
    }

    private void setUpToolbar() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        if (toolbar != null) {
            Menu menu = toolbar.getMenu();
            if (menu != null) {
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(true);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpToolbar();
        updateSongUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        view.removeOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
        onLayoutChangeListenerFragmentSongButtons = null;
        ImageButton buttonBad = binding.buttonThumbDown;
        ImageButton buttonGood = binding.buttonThumbUp;
        ImageButton buttonShuffle = binding.imageButtonShuffle;
        ImageButton buttonPrev = binding.imageButtonPrev;
        ImageButton buttonPause = binding.imageButtonPlayPause;
        ImageButton buttonNext = binding.imageButtonNext;
        ImageButton buttonLoop = binding.imageButtonRepeat;
        buttonBad.setOnClickListener(null);
        buttonGood.setOnClickListener(null);
        buttonShuffle.setOnClickListener(null);
        buttonPrev.setOnClickListener(null);
        buttonPause.setOnClickListener(null);
        buttonNext.setOnClickListener(null);
        buttonLoop.setOnClickListener(null);
        buttonBad.setOnTouchListener(null);
        buttonGood.setOnTouchListener(null);
        buttonShuffle.setOnTouchListener(null);
        buttonPrev.setOnTouchListener(null);
        buttonPause.setOnTouchListener(null);
        buttonNext.setOnTouchListener(null);
        buttonLoop.setOnTouchListener(null);
        buttonNext.setOnLongClickListener(null);
        onClickListenerFragmentSong = null;
        onTouchListenerFragmentSongButtons = null;
        onLongClickListener = null;
        shutDownSeekBarUpdater();
        SeekBar seekBar = binding.seekBar;
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(null);
        }
        onSeekBarChangeListener = null;
        runnableSeekBarUpdater = null;
        runnableSongArtUpdater = null;
        activityMain.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        viewModelActivityMain.getCurrentSong().removeObservers(this);
        observerCurrentSong = null;
        viewModelActivityMain.isPlaying().removeObservers(this);
        observerIsPlaying = null;
        viewModelActivityMain = null;
        binding = null;
    }

}
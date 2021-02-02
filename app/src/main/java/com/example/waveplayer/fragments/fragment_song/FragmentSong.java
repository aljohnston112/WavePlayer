package com.example.waveplayer.fragments.fragment_song;

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
import com.example.waveplayer.activity_main.OnSeekBarChangeListener;
import com.example.waveplayer.activity_main.RunnableSongArtUpdater;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentSongBinding;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.BitmapLoader;
import com.example.waveplayer.media_controller.MediaPlayerWUri;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.Song;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FragmentSong extends Fragment
        implements OnSeekBarChangeListener.OnSeekBarChangeCallback,
        RunnableSongArtUpdater.SongArtUpdateCallback {

    private FragmentSongBinding binding;

    private ViewModelActivityMain viewModelActivityMain;

    private BroadcastReceiver broadcastReceiver;

    private OnSeekBarChangeListener onSeekBarChangeListener;

    private View.OnClickListener onClickListenerFragmentSong;
    private View.OnLongClickListener onLongClickListener;
    private View.OnTouchListener onTouchListenerFragmentSongButtons;
    private View.OnLayoutChangeListener onLayoutChangeListenerFragmentSongButtons;

    // For updating the SeekBar
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private Runnable runnableSeekBarUpdater;

    private RunnableSongArtUpdater runnableSongArtUpdater;

    private Observer<AudioUri> observerCurrentSong;

    private Observer<Boolean> observerIsPlaying;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSongBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.hideKeyboard(view);
        updateMainContent();
        onSeekBarChangeListener = new OnSeekBarChangeListener(this);
        runnableSongArtUpdater = new RunnableSongArtUpdater(this);
        updateSongUI();
        observerCurrentSong = s -> {
            updateSongUI();
        };
        viewModelActivityMain.getCurrentSong().observe(getViewLifecycleOwner(), observerCurrentSong);
        observerIsPlaying = b -> {
            updateSongPlayButton();
        };
        viewModelActivityMain.isPlaying().observe(getViewLifecycleOwner(), observerIsPlaying);
        setUpButtons();
        activityMain.isSong(true);
        activityMain.setSongToAddToQueue(activityMain.getCurrentAudioUri().id);
        setUpBroadcastReceiver();
    }

    private void updateMainContent() {
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
        viewModelActivityMain.showFab(false);
        setUpToolbar();
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

    private void updateSeekBar() {
        // Log.v(TAG, "updateSeekBar start");
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        Context context = activityMain.getApplicationContext();
        SeekBar seekBar = binding.seekBar;
        if (seekBar != null) {
            AudioUri audioUri = activityMain.getCurrentAudioUri();
            final int maxMillis;
            if (audioUri != null) {
                maxMillis = audioUri.getDuration(context);
            } else {
                maxMillis = 9999;
            }
            seekBar.setMax(maxMillis);
            seekBar.setProgress(activityMain.getCurrentTime());
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
            setUpSeekBarUpdater();
        }
        // Log.v(TAG, "updateSeekBar end");
    }

    private void setUpSeekBarUpdater() {
        // Log.v(TAG, "setUpSeekBarUpdater start");
        SeekBar seekBar = binding.seekBar;
        TextView textViewCurrent = binding.editTextCurrentTime;
        updateSeekBarUpdater(seekBar, textViewCurrent);
        // Log.v(TAG, "setUpSeekBarUpdater end");
    }

    public void updateSeekBarUpdater(SeekBar seekBar, TextView textViewCurrent) {
        // Log.v(TAG, "updateSeekBarUpdater start");
        ActivityMain activityMain = (ActivityMain) requireActivity();
        shutDownSeekBarUpdater();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        MediaPlayerWUri mediaPlayerWUri = activityMain.getCurrentMediaPlayerWUri();
        if (mediaPlayerWUri != null) {
            runnableSeekBarUpdater = () -> seekBar.post(() -> {
                if (mediaPlayerWUri.isPlaying()) {
                    int currentMilliseconds = mediaPlayerWUri.getCurrentPosition();
                    seekBar.setProgress(currentMilliseconds);
                    final String currentTime = String.format(getResources().getConfiguration().locale,
                            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(currentMilliseconds),
                            TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds) -
                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(currentMilliseconds)),
                            TimeUnit.MILLISECONDS.toSeconds(currentMilliseconds) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds)));
                    textViewCurrent.setText(currentTime);
                }
            });
            scheduledExecutorService.scheduleAtFixedRate(
                    runnableSeekBarUpdater, 0L, 1L, TimeUnit.SECONDS);
        }
        // Log.v(TAG, "updateSeekBarUpdater end");
    }

    public void shutDownSeekBarUpdater() {
        // Log.v(TAG, "shutDownSeekBarUpdater start");
        runnableSeekBarUpdater = null;
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = null;
        }
        // Log.v(TAG, "shutDownSeekBarUpdater end");
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
        if (imageViewSongArt != null) {
            imageViewSongArt.post(runnableSongArtUpdater);
        }
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
        if (textViewCurrent != null) {
            textViewCurrent.setText(stringCurrentTime);
        }
        TextView textViewEnd = binding.editTextEndTime;
        if (textViewEnd != null) {
            textViewEnd.setText(stringEndTime);
        }
        // Log.v(TAG, "updateTextViewTimes end");
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

    private String formatMillis(int millis) {
        // Log.v(TAG, "formatMillis start and end");
        return String.format(getResources().getConfiguration().locale,
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    private void setUpBroadcastReceiver() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu));
        broadcastReceiver = new BroadcastReceiverOnServiceConnected() {
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

    @SuppressLint("ClickableViewAccessibility")
    private void setUpButtons() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        final ImageButton buttonBad = view.findViewById(R.id.button_thumb_down);
        final ImageButton buttonGood = view.findViewById(R.id.button_thumb_up);
        final ImageButton buttonShuffle = view.findViewById(R.id.imageButtonShuffle);
        final ImageButton buttonPrev = view.findViewById(R.id.imageButtonPrev);
        final ImageButton buttonPause = view.findViewById(R.id.imageButtonPlayPause);
        final ImageButton buttonNext = view.findViewById(R.id.imageButtonNext);
        final ImageButton buttonLoop = view.findViewById(R.id.imageButtonRepeat);
        onClickListenerFragmentSong = clickedView -> {
            synchronized (ActivityMain.lock) {
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
                        activityMain.getCurrentPlaylist().good(activityMain.getApplicationContext(),
                                song, activityMain.getPercentChangeUp(), true);
                        activityMain.saveFile();
                    }
                } else if (clickedView.getId() == R.id.imageButtonShuffle) {
                    ImageButton imageButton = (ImageButton) clickedView;
                    if (activityMain.shuffling()) {
                        activityMain.shuffling(false);
                        imageButton.setImageResource(R.drawable.ic_shuffle_white_24dp);
                    } else {
                        activityMain.shuffling(true);
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
                    if (activityMain.loopingOne()) {
                        activityMain.loopingOne(false);
                        imageButton.setImageResource(R.drawable.repeat_white_24dp);
                    } else if (activityMain.looping()) {
                        activityMain.looping(false);
                        activityMain.loopingOne(true);
                        imageButton.setImageResource(R.drawable.repeat_one_black_24dp);
                    } else {
                        activityMain.looping(true);
                        activityMain.loopingOne(false);
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
        buttonNext.setLongClickable(true);
        onLongClickListener = v -> {
            // TODO
            activityMain.getCurrentPlaylist().globalBad(
                    activityMain.getSong(activityMain.getCurrentAudioUri().id),
                    activityMain.getPercentChangeDown());
            return true;

        };
        buttonNext.setOnLongClickListener(onLongClickListener);
        buttonLoop.setOnClickListener(onClickListenerFragmentSong);
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
        if (activityMain.shuffling()) {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp);
        } else {
            buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp);
        }
        if (activityMain.loopingOne()) {
            buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp);
        } else if (activityMain.looping()) {
            buttonLoop.setImageResource(R.drawable.repeat_black_24dp);
        } else {
            buttonLoop.setImageResource(R.drawable.repeat_white_24dp);
        }
        onLayoutChangeListenerFragmentSongButtons =
                new View.OnLayoutChangeListener(){
                    @Override
                    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if(activityMain.fragmentSongVisible()) {
                            setUpGood(view);
                            setUpBad(view);
                            setUpShuffle(view);
                            setUpPrev(view);
                            setUpPlay(view);
                            setUpNext(view);
                            setUpLoop(view);
                        }
                    }

                };
        view.addOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        final ImageButton buttonBad = view.findViewById(R.id.button_thumb_down);
        final ImageButton buttonGood = view.findViewById(R.id.button_thumb_up);
        final ImageButton buttonShuffle = view.findViewById(R.id.imageButtonShuffle);
        final ImageButton buttonPrev = view.findViewById(R.id.imageButtonPrev);
        final ImageButton buttonPause = view.findViewById(R.id.imageButtonPlayPause);
        final ImageButton buttonNext = view.findViewById(R.id.imageButtonNext);
        final ImageButton buttonLoop = view.findViewById(R.id.imageButtonRepeat);
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
        view.removeOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
        onLayoutChangeListenerFragmentSongButtons = null;
        activityMain.setSongToAddToQueue(null);
        shutDownSeekBarUpdater();
        SeekBar seekBar = binding.seekBar;
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(null);
        }
        onSeekBarChangeListener = null;
        runnableSongArtUpdater = null;
        binding = null;
        viewModelActivityMain.getCurrentSong().removeObservers(this);
        observerCurrentSong = null;
        viewModelActivityMain.isPlaying().removeObservers(this);
        observerIsPlaying = null;
        viewModelActivityMain = null;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.seekTo(seekBar.getProgress());
    }

    @Override
    public void updateSongArtRun() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
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
                    Bitmap bitmapResized =
                            BitmapLoader.getResizedBitmap(bitmapDrawable, songArtWidth, songArtHeight);
                    bitmapDrawable.recycle();
                    imageViewSongArt.setImageBitmap(bitmapResized);
                }
            } else {
                imageViewSongArt.setImageBitmap(bitmap);
            }
        }
    }

    private void setUpGood(View view) {
        ImageView imageView = view.findViewById(R.id.button_thumb_up);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(),
                R.drawable.thumb_up_alt_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpBad(View view) {
        ImageView imageView = view.findViewById(R.id.button_thumb_down);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(),
                R.drawable.thumb_down_alt_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpShuffle(View view) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = view.findViewById(R.id.imageButtonShuffle);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable;
        if (activityMain.shuffling()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.ic_shuffle_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.ic_shuffle_white_24dp, null);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpNext(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonNext);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.skip_next_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpPlay(View view) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = view.findViewById(R.id.imageButtonPlayPause);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable;
        if (activityMain.isPlaying()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.pause_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.play_arrow_black_24dp, null);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpPrev(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonPrev);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.skip_previous_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpLoop(View view) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = view.findViewById(R.id.imageButtonRepeat);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable;

        if (activityMain.loopingOne()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.repeat_one_black_24dp, null);
        } else if (activityMain.looping()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.repeat_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.repeat_white_24dp, null);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

}
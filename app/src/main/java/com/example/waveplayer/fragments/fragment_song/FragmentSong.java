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
import com.example.waveplayer.media_controller.MediaController;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.MediaPlayerWUri;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.service_main.RunnableSeekBarUpdater;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FragmentSong extends Fragment
implements OnSeekBarChangeListener.OnSeekBarChangeCallback,
        RunnableSongArtUpdater.SongArtUpdateCallback {

    private FragmentSongBinding binding;

    private ViewModelActivityMain viewModelActivityMain;

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;
    private BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    private OnSeekBarChangeListener onSeekBarChangeListener;

    private OnClickListenerFragmentSong onClickListenerFragmentSong;
    private View.OnLongClickListener onLongClickListener;
    private OnTouchListenerFragmentSongButtons onTouchListenerFragmentSongButtons;
    private OnLayoutChangeListenerFragmentSongButtons onLayoutChangeListenerFragmentSongButtons;

    // For updating the SeekBar
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private RunnableSeekBarUpdater runnableSeekBarUpdater;

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
        setUpBroadcastReceiverServiceConnected();
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
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
            AudioUri audioUri = MediaController.getInstance(context).getCurrentAudioUri();
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
        MediaController mediaController = MediaController.getInstance(
                requireActivity().getApplicationContext());
        shutDownSeekBarUpdater();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        MediaPlayerWUri mediaPlayerWUri =
                mediaController.getMediaPlayerWUri(mediaController.getCurrentAudioUri().id);
        if (mediaPlayerWUri != null) {
            runnableSeekBarUpdater = new RunnableSeekBarUpdater(
                    mediaPlayerWUri,
                    seekBar, textViewCurrent,
                    getResources().getConfiguration().locale);
            scheduledExecutorService.scheduleAtFixedRate(
                    runnableSeekBarUpdater, 0L, 1L, TimeUnit.SECONDS);
        }
        // Log.v(TAG, "updateSeekBarUpdater end");
    }

    public void shutDownSeekBarUpdater() {
        // Log.v(TAG, "shutDownSeekBarUpdater start");
        if (runnableSeekBarUpdater != null) {
            runnableSeekBarUpdater.shutDown();
            runnableSeekBarUpdater = null;
        }
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

    private void setUpBroadcastReceiverServiceOnOptionsMenuCreated() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_on_create_options_menu));
        broadcastReceiverOptionsMenuCreated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpToolbar();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
    }

    private void setUpBroadcastReceiverServiceConnected() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpButtons();
                updateSongUI();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
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
        onClickListenerFragmentSong = new OnClickListenerFragmentSong(activityMain);
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
        onTouchListenerFragmentSongButtons = new OnTouchListenerFragmentSongButtons();
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
                new OnLayoutChangeListenerFragmentSongButtons(activityMain);
        view.addOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
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
    public void updateSongArtRun(){
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageViewSongArt = binding.imageViewSongArt;
            int songArtHeight = imageViewSongArt.getHeight();
            int songArtWidth = imageViewSongArt.getWidth();
            if (songArtWidth > songArtHeight) {
                songArtWidth = songArtHeight;
            } else {
                songArtHeight = songArtWidth;
            }
            if(songArtHeight > 0 && songArtWidth > 0) {
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

}
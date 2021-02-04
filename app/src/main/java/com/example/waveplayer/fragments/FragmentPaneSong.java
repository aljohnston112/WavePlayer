package com.example.waveplayer.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentPaneSongBinding;
import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.BitmapLoader;
import com.example.waveplayer.random_playlist.AudioUri;

public class FragmentPaneSong extends Fragment {

    private FragmentPaneSongBinding binding;

    private ViewModelActivityMain viewModelActivityMain;

    private BroadcastReceiver broadcastReceiverOnServiceConnected;

    private NavController.OnDestinationChangedListener onDestinationChangedListenerSongPane;
    private View.OnLayoutChangeListener onLayoutChangeListenerSongPane;
    private View.OnClickListener onClickListenerSongPane;

    private Runnable runnableSongPaneArtUpdater;

    private Observer<Boolean> observerIsPlaying;
    private Observer<AudioUri> observerCurrentSong;

    private int songArtWidth = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        viewModelActivityMain =
                new ViewModelProvider(activityMain).get(ViewModelActivityMain.class);
        setUpObservers();
        binding = FragmentPaneSongBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void setUpObservers() {
        observerCurrentSong = this::updateSongUI;
        viewModelActivityMain.getCurrentSong().observe(getViewLifecycleOwner(), observerCurrentSong);
        observerIsPlaying = this::setUpPlayButton;
        viewModelActivityMain.isPlaying().observe(getViewLifecycleOwner(), observerIsPlaying);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = (ActivityMain) requireActivity();
        setUpBroadcastReceiver();
        setUpRunnableSongArtUpdater();
        setUpOnLayoutChangeListener(view);
        setUpOnDestinationChangedListener();
        linkSongPaneButtons();
        updateSongUI(activityMain.getCurrentAudioUri());
    }

    private void setUpBroadcastReceiver() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO
                updateSongUI(activityMain.getCurrentAudioUri());
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void setUpRunnableSongArtUpdater() {
        runnableSongPaneArtUpdater = () -> {
            ActivityMain activityMain = (ActivityMain) requireActivity();
            ImageView imageViewSongPaneSongArt = binding.imageViewSongPaneSongArt;
            Bitmap bitmapSongArt = BitmapLoader.getThumbnail(
                    activityMain.getCurrentUri(), songArtWidth, songArtWidth,
                    activityMain.getApplicationContext());
            if (bitmapSongArt != null) {
                imageViewSongPaneSongArt.setImageBitmap(bitmapSongArt);
            } else {
                Bitmap defaultBitmap = getDefaultBitmap(songArtWidth, songArtWidth);
                if (defaultBitmap != null) {
                    imageViewSongPaneSongArt.setImageBitmap(defaultBitmap);
                }
            }
        };
    }

    private Bitmap getDefaultBitmap(int songArtWidth, int songArtHeight) {
        // TODO cache bitmap
        ImageView imageViewSongPaneSongArt = binding.imageViewSongPaneSongArt;
        Drawable drawableSongArt = ResourcesCompat.getDrawable(
                imageViewSongPaneSongArt.getResources(), R.drawable.music_note_black_48dp, null);
        if (drawableSongArt != null) {
            Bitmap bitmapSongArt;
            drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight);
            bitmapSongArt = Bitmap.createBitmap(
                    songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapSongArt);
            drawableSongArt.draw(canvas);
            return bitmapSongArt;
        }
        return null;
    }

    private void setUpOnLayoutChangeListener(View view) {
        onLayoutChangeListenerSongPane =
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    ActivityMain activityMain = ((ActivityMain) requireActivity());
                    if (activityMain.findViewById(R.id.fragmentSongPane).getVisibility() == View.VISIBLE &&
                            !activityMain.fragmentSongVisible()) {
                        if (songArtWidth == 0) {
                            ImageView imageViewSongPaneSongArt = binding.imageViewSongPaneSongArt;
                            songArtWidth = imageViewSongPaneSongArt.getWidth();
                        }
                        updateSongUI(activityMain.getCurrentAudioUri());
                        setUpPrev();
                        setUpPlayButton(activityMain.isPlaying());
                        setUpNext();
                    }
                };
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane);
    }

    public void updateSongUI(AudioUri currentAudioUri) {
        // Log.v(TAG, "updateSongPaneUI start");
        ActivityMain activityMain = (ActivityMain) requireActivity();
        if (!activityMain.fragmentSongVisible() && activityMain.songInProgress()) {
            // Log.v(TAG, "updating the song pane UI");
            updateSongPaneName(currentAudioUri.title);
            updateSongPaneArt();
        } else {
            // Log.v(TAG, "not updating the song pane UI");
        }
        // Log.v(TAG, "updateSongPaneUI end");
    }

    private void updateSongPaneName(String title) {
        // Log.v(TAG, "updateSongPaneName start");
        TextView textViewSongPaneSongName = binding.textViewSongPaneSongName;
        textViewSongPaneSongName.setText(title);
        // Log.v(TAG, "updateSongPaneName end");
    }

    private void updateSongPaneArt() {
        // Log.v(TAG, "updateSongPaneArt start");
        final ImageView imageViewSongPaneSongArt = binding.imageViewSongPaneSongArt;
        imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater);
        // Log.v(TAG, "updateSongPaneArt end");
    }

    private void setUpPrev() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = binding.imageButtonSongPanePrev;
        Drawable drawable = ResourcesCompat.getDrawable(
                activityMain.getResources(), R.drawable.skip_previous_black_24dp, null);
        if (drawable != null && songArtWidth > 0) {
            drawable.setBounds(0, 0, songArtWidth, songArtWidth);
            Bitmap bitmap = Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            imageView.setImageBitmap(bitmap);
            bitmap.recycle();
        }
    }

    private void setUpPlayButton(boolean isPlaying) {
        ImageView imageView = binding.imageButtonSongPanePlayPause;
        Drawable drawable;
        if (isPlaying) {
            drawable = ResourcesCompat.getDrawable(
                    getResources(), R.drawable.pause_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(
                    getResources(), R.drawable.play_arrow_black_24dp, null);
        }
        if (drawable != null && songArtWidth > 0) {
            drawable.setBounds(0, 0, songArtWidth, songArtWidth);
            Bitmap bitmap = Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            imageView.setImageBitmap(bitmap);
            bitmap.recycle();
        }
    }

    private void setUpNext() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = binding.imageButtonSongPaneNext;
        Drawable drawable = ResourcesCompat.getDrawable(
                activityMain.getResources(), R.drawable.skip_next_black_24dp, null);
        if (drawable != null && songArtWidth > 0) {
            drawable.setBounds(0, 0, songArtWidth, songArtWidth);
            Bitmap bitmap = Bitmap.createBitmap(songArtWidth, songArtWidth, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            imageView.setImageBitmap(bitmap);
            bitmap.recycle();
        }
    }

    private void setUpOnDestinationChangedListener() {
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong started");
        ActivityMain activityMain = (ActivityMain) requireActivity();
        onDestinationChangedListenerSongPane = (controller, destination, arguments) -> {
            if (destination.getId() != R.id.fragmentSong) {
                if (activityMain.songInProgress()) {
                    activityMain.fragmentSongVisible(false);
                    activityMain.showSongPane();
                }
            } else {
                activityMain.fragmentSongVisible(true);
                activityMain.hideSongPane();
            }
        };
        FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerSongPane);
        }
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong ended");
    }

    private void linkSongPaneButtons() {
        // Log.v(TAG, "linking song pane buttons");
        onClickListenerSongPane = v -> {
            ActivityMain activityMain = (ActivityMain) requireActivity();
            synchronized (ActivityMain.MUSIC_CONTROL_LOCK) {
                if (v.getId() == R.id.imageButtonSongPaneNext) {
                    activityMain.playNext();
                } else if (v.getId() == R.id.imageButtonSongPanePlayPause) {
                    activityMain.pauseOrPlay();
                } else if (v.getId() == R.id.imageButtonSongPanePrev) {
                    activityMain.playPrevious();
                } else if (v.getId() == R.id.textViewSongPaneSongName ||
                        v.getId() == R.id.imageViewSongPaneSongArt) {
                    activityMain.navigateTo(R.id.fragmentSong);
                }
            }
        };
        binding.imageButtonSongPaneNext.setOnClickListener(onClickListenerSongPane);
        binding.imageButtonSongPanePlayPause.setOnClickListener(onClickListenerSongPane);
        binding.imageButtonSongPanePrev.setOnClickListener(onClickListenerSongPane);
        binding.textViewSongPaneSongName.setOnClickListener(onClickListenerSongPane);
        binding.imageViewSongPaneSongArt.setOnClickListener(onClickListenerSongPane);
        // Log.v(TAG, "done linking song pane buttons");
    }

    public void removeListeners() {
        // Log.v(TAG, "removeListeners started");
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = requireView();
        view.removeOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        onLayoutChangeListenerSongPane = null;
        FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerSongPane);
        }
        onDestinationChangedListenerSongPane = null;
        binding.imageButtonSongPaneNext.setOnClickListener(null);
        binding.imageButtonSongPanePlayPause.setOnClickListener(null);
        binding.imageButtonSongPanePrev.setOnClickListener(null);
        binding.textViewSongPaneSongName.setOnClickListener(null);
        binding.imageViewSongPaneSongArt.setOnClickListener(null);
        onClickListenerSongPane = null;
        // Log.v(TAG, "removeListeners ended");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        removeListeners();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        runnableSongPaneArtUpdater = null;
        viewModelActivityMain.getCurrentSong().removeObservers(this);
        observerCurrentSong = null;
        viewModelActivityMain.isPlaying().removeObservers(this);
        observerIsPlaying = null;
        viewModelActivityMain = null;
        binding = null;
    }

}
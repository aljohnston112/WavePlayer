package com.example.waveplayer.fragments.fragment_pane_song;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.OnDestinationChangedListenerSongPane;
import com.example.waveplayer.activity_main.RunnableSongPaneArtUpdater;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentPaneSongBinding;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.BitmapLoader;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.random_playlist.AudioUri;

public class FragmentPaneSong extends Fragment
        implements OnDestinationChangedListenerSongPane.OnDestinationChangedCallback,
        OnClickListenerSongPane.OnClickCallback {

    private ViewModelActivityMain viewModelActivityMain;

    private FragmentPaneSongBinding binding;

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private OnLayoutChangeListenerSongPane onLayoutChangeListenerSongPane;

    private OnDestinationChangedListenerSongPane onDestinationChangedListenerSongPane;

    private OnClickListenerSongPane onClickListenerSongPane;

    private RunnableSongPaneArtUpdater runnableSongPaneArtUpdater;

    private Observer<AudioUri> observerCurrentSong;

    private Observer<Boolean> observerIsPlaying;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPaneSongBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        onLayoutChangeListenerSongPane = new OnLayoutChangeListenerSongPane(this);
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        setUpBroadcastReceiver();
        setUpDestinationChangedListenerForPaneSong();
        linkSongPaneButtons();
        runnableSongPaneArtUpdater = new RunnableSongPaneArtUpdater((ActivityMain) requireActivity());
        observerCurrentSong = s -> {
            updateSongPaneUI();
        };
        viewModelActivityMain.getCurrentSong().observe(getViewLifecycleOwner(), observerCurrentSong);
        observerIsPlaying = b -> {
            updateSongPanePlayButton();
        };
        viewModelActivityMain.isPlaying().observe(getViewLifecycleOwner(), observerIsPlaying);
        updateSongPaneUI();
    }

    private void setUpDestinationChangedListenerForPaneSong() {
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong started");
        onDestinationChangedListenerSongPane =
                new OnDestinationChangedListenerSongPane(this);
        ActivityMain activityMain = (ActivityMain) requireActivity();
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
        onClickListenerSongPane = new OnClickListenerSongPane(this);
        binding.imageButtonSongPaneNext.setOnClickListener(onClickListenerSongPane);
        binding.imageButtonSongPanePlayPause.setOnClickListener(onClickListenerSongPane);
        binding.imageButtonSongPanePrev.setOnClickListener(onClickListenerSongPane);
        binding.textViewSongPaneSongName.setOnClickListener(onClickListenerSongPane);
        binding.imageViewSongPaneSongArt.setOnClickListener(onClickListenerSongPane);
        // Log.v(TAG, "done linking song pane buttons");
    }

    private void setUpBroadcastReceiver() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO
                updateSongPaneUI();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void updateSongPanePlayButton() {
        setUpPlay(getView());
    }

    public void updateSongPaneUI() {
        // Log.v(TAG, "updateSongPaneUI start");
        ActivityMain activityMain = (ActivityMain) requireActivity();
        if (activityMain.songInProgress() && activityMain.getCurrentAudioUri() != null) {
            // Log.v(TAG, "updating the song pane UI");
            updateSongPanePlayButton();
            updateSongPaneName();
            updateSongPaneArt();
        } else {
            // Log.v(TAG, "not updating the song pane UI");
        }
        // Log.v(TAG, "updateSongPaneUI end");
    }


    private void updateSongPaneName() {
        // Log.v(TAG, "updateSongPaneName start");
        ActivityMain activityMain = (ActivityMain) requireActivity();
        TextView textViewSongPaneSongName = binding.textViewSongPaneSongName;
            textViewSongPaneSongName.setText(activityMain.getCurrentAudioUri().title);
        // Log.v(TAG, "updateSongPaneName end");
    }

    private void updateSongPaneArt() {
        // Log.v(TAG, "updateSongPaneArt start");
        final ImageView imageViewSongPaneSongArt = binding.imageViewSongPaneSongArt;
            imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater);
        // Log.v(TAG, "updateSongPaneArt end");
    }

    public void removeListeners() {
        // Log.v(TAG, "removeListeners started");
        ActivityMain activityMain = (ActivityMain) requireActivity();
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
        View view = getView();
        removeListeners();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        view.removeOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        onLayoutChangeListenerSongPane = null;
        runnableSongPaneArtUpdater = null;
        viewModelActivityMain.getCurrentSong().removeObservers(this);
        observerCurrentSong = null;
        viewModelActivityMain.isPlaying().removeObservers(this);
        observerIsPlaying = null;
        viewModelActivityMain = null;
        binding = null;
    }

    @Override
    public void onDestinationChanged(@NonNull NavDestination destination) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        if (destination.getId() != R.id.fragmentSong) {
            if (activityMain.songInProgress()) {
                activityMain.fragmentSongVisible(false);
                activityMain.showSongPane();
            }
        } else {
            activityMain.fragmentSongVisible(true);
            activityMain.hideSongPane();
        }
    }

    @Override
    public void onClick(View v) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        synchronized (ActivityMain.lock) {
            if (v.getId() == R.id.imageButtonSongPaneNext) {
                activityMain.playNext();
            } else if (v.getId() == R.id.imageButtonSongPanePlayPause) {
                activityMain.pauseOrPlay();
            } else if (v.getId() == R.id.imageButtonSongPanePrev) {
                activityMain.playPrevious();
            } else if (v.getId() == R.id.textViewSongPaneSongName ||
                    v.getId() == R.id.imageViewSongPaneSongArt) {
                openFragmentSong();
            }
        }
    }

    private void openFragmentSong() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).navigate(R.id.fragmentSong);
        }
    }

    private void setUpPlay(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonSongPanePlayPause);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable;
        ActivityMain activityMain = (ActivityMain) requireActivity();
        if (activityMain.isPlaying()) {
            drawable = ResourcesCompat.getDrawable(
                    getResources(), R.drawable.pause_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(
                    getResources(), R.drawable.play_arrow_black_24dp, null);
        }
        if (drawable != null && width > 0) {
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
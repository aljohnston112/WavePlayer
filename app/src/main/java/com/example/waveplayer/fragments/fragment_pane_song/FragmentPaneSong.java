package com.example.waveplayer.fragments.fragment_pane_song;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
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

    private ViewModelActivityMain viewModelActivityMain;

    private FragmentPaneSongBinding binding;

    private BroadcastReceiver broadcastReceiverOnServiceConnected;

    private View.OnLayoutChangeListener onLayoutChangeListenerSongPane;

    private NavController.OnDestinationChangedListener onDestinationChangedListenerSongPane;

    private View.OnClickListener onClickListenerSongPane;

    private Runnable runnableSongPaneArtUpdater;

    private Observer<AudioUri> observerCurrentSong;

    private Observer<Boolean> observerIsPlaying;

    private int songArtWidth;

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
        ActivityMain activityMain = (ActivityMain) requireActivity();
        viewModelActivityMain =
                new ViewModelProvider(activityMain).get(ViewModelActivityMain.class);
        onLayoutChangeListenerSongPane =
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    ActivityMain activityMain1 = ((ActivityMain) requireActivity());
                    if (activityMain1.findViewById(R.id.fragmentSongPane).getVisibility() == View.VISIBLE &&
                            !activityMain1.fragmentSongVisible()) {
                        if (songArtWidth == 0) {
                            ImageView imageViewSongPaneSongArt =
                                    activityMain1.findViewById(R.id.imageViewSongPaneSongArt);
                            songArtWidth = imageViewSongPaneSongArt.getWidth();
                        }
                        updateSongPaneUI();
                        setUpPrev(v);
                        setUpPlayButton(v);
                        setUpNext(v);
                    }
                };
        ImageView imageViewSongPaneSongArt = activityMain.findViewById(R.id.imageViewSongPaneSongArt);
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        setUpBroadcastReceiver();
        setUpDestinationChangedListenerForPaneSong();
        linkSongPaneButtons();
        runnableSongPaneArtUpdater = () -> {
            int songArtHeight = songArtWidth;
            Bitmap bitmapSongArt = BitmapLoader.getThumbnail(
                    activityMain.getCurrentUri(), songArtWidth, songArtHeight, activityMain.getApplicationContext());
            if (imageViewSongPaneSongArt != null) {
                if (bitmapSongArt != null) {
                    imageViewSongPaneSongArt.setImageBitmap(bitmapSongArt);
                } else {
                    Bitmap defaultBitmap = getDefaultBitmap(songArtWidth, songArtHeight);
                    if (defaultBitmap != null) {
                        imageViewSongPaneSongArt.setImageBitmap(defaultBitmap);
                    }
                }
            }
        };
        observerCurrentSong = s -> {
            updateSongPaneUI();
        };
        viewModelActivityMain.getCurrentSong().observe(getViewLifecycleOwner(), observerCurrentSong);
        observerIsPlaying = b -> {
            setUpPlayButton(getView());
        };
        viewModelActivityMain.isPlaying().observe(getViewLifecycleOwner(), observerIsPlaying);
        updateSongPaneUI();
    }

    private void setUpDestinationChangedListenerForPaneSong() {
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong started");
        onDestinationChangedListenerSongPane = new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
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
        };
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
        onClickListenerSongPane = v -> {
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
        };
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
        broadcastReceiverOnServiceConnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO
                updateSongPaneUI();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    public void updateSongPaneUI() {
        // Log.v(TAG, "updateSongPaneUI start");
        ActivityMain activityMain = (ActivityMain) requireActivity();
        if (activityMain.songInProgress() && activityMain.getCurrentAudioUri() != null) {
            // Log.v(TAG, "updating the song pane UI");
            setUpPlayButton(getView());
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

    private void openFragmentSong() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        FragmentManager fragmentManager = activityMain.getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).navigate(R.id.fragmentSong);
        }
    }

    private void setUpPlayButton(View view) {
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

    private Bitmap getDefaultBitmap(int songArtWidth, int songArtHeight) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageViewSongPaneSongArt = activityMain.findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null) {
            Drawable drawableSongArt = ResourcesCompat.getDrawable(
                    imageViewSongPaneSongArt.getResources(), R.drawable.music_note_black_48dp, null);
            if (drawableSongArt != null) {
                Bitmap bitmapSongArt;
                drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight);
                bitmapSongArt = Bitmap.createBitmap(
                        songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmapSongArt);
                drawableSongArt.draw(canvas);
                Bitmap bitmapSongArtResized = BitmapLoader.getResizedBitmap(
                        bitmapSongArt, songArtWidth, songArtHeight);
                bitmapSongArt.recycle();
                return bitmapSongArtResized;
            }
        }
        return null;
    }

    private void setUpNext(View view) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = view.findViewById(R.id.imageButtonSongPaneNext);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable = ResourcesCompat.getDrawable(
                activityMain.getResources(), R.drawable.skip_next_black_24dp, null);
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

    private void setUpPrev(View view) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ImageView imageView = view.findViewById(R.id.imageButtonSongPanePrev);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable = ResourcesCompat.getDrawable(
                activityMain.getResources(), R.drawable.skip_previous_black_24dp, null);
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
package com.example.waveplayer.activity_main;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.R;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.fragments.fragment_pane_song.OnClickListenerSongPane;
import com.example.waveplayer.media_controller.MediaController;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.SaveFile;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.service_main.ServiceMain;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.activity_main.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;

public class ActivityMain extends AppCompatActivity {

    // TODO help page
    // TODO check for leaks
    // TODO warn user about resetting probabilities
    // TODO lower probabilities without resetting
    // TODO create a backup file somewhere
    // TODO allow user to create backup
    // TODO start shuffle from user picked playlist when play button in notification is tapped

    // TODO AFTER RELEASE
    // Open current song in folder as a menu action
    // Setting to not keep playing after queue is done

    static final String TAG = "ActivityMain";

    public final static Object lock = new Object();

    public static final int MENU_ACTION_RESET_PROBS_INDEX = 0;
    public static final int MENU_ACTION_ADD_TO_PLAYLIST_INDEX = 1;
    public static final int MENU_ACTION_SEARCH_INDEX = 2;
    public static final int MENU_ACTION_ADD_TO_QUEUE = 3;

    private static final String KEY_BOOLEAN_LOADED = "KEY_BOOLEAN_LOADED";

    private ServiceMain serviceMain;

    private MediaController mediaController;

    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private boolean loaded = false;

    public void loaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void setServiceMain(ServiceMain serviceMain) {
        // Log.v(TAG, "setServiceMain started");
        this.serviceMain = serviceMain;
        setUpAfterServiceConnection();
        // Log.v(TAG, "setServiceMain ended");
    }

    private void setUpAfterServiceConnection() {
        // Log.v(TAG, "setUpAfterServiceConnection started");
        if (loaded) {
            mediaController = MediaController.getInstance(getApplicationContext());
            if (!fragmentSongVisible()) {
                if (MediaController.getInstance(getApplicationContext()).isPlaying()) {
                    navigateTo(R.id.fragmentSong);
                } else {
                    navigateTo(R.id.FragmentTitle);
                }
            }
        } else {
            mediaController = MediaController.getInstance(getApplicationContext());
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            if (fragment != null) {
                NavHostFragment.findNavController(fragment).popBackStack(R.id.fragmentLoading, true);
            }
        }


        serviceMain.permissionGranted();
        setUpBroadcastReceivers();
        runnableSongArtUpdater = new RunnableSongArtUpdater(this);
        runnableSongPaneArtUpdater = new RunnableSongPaneArtUpdater(this);
        setUpSongPane();
        // Log.v(TAG, "setUpAfterServiceConnection ended");
    }

    public void serviceDisconnected() {
        // Log.v(TAG, "serviceDisconnected started");
        unregisterReceivers();
        removeListeners();
        runnableSongArtUpdater = null;
        runnableSongPaneArtUpdater = null;
        mediaController = null;
        // Log.v(TAG, "serviceDisconnected end");
    }

    private ServiceConnection connectionServiceMain;

    private BroadcastReceiverOnCompletion broadcastReceiverOnCompletion;
    private BroadcastReceiverNotificationButtonsForActivityMain
            broadcastReceiverNotificationButtonsForActivityMain;

    private OnDestinationChangedListenerToolbar onDestinationChangedListenerToolbar;

    private OnDestinationChangedListenerPanes onDestinationChangedListenerPanes;

    private OnClickListenerSongPane onClickListenerSongPane;

    private OnSeekBarChangeListener onSeekBarChangeListener;

    private Runnable runnableUIUpdate;

    private RunnableSongArtUpdater runnableSongArtUpdater;

    private RunnableSongPaneArtUpdater runnableSongPaneArtUpdater;

    private boolean isSong;

    public void isSong(boolean isSong) {
        // Log.v(TAG, "isSong started");
        this.isSong = isSong;
        // Log.v(TAG, "isSong ended");
    }

    private Long songToAddToQueue;

    public void setSongToAddToQueue(Long songID) {
        // Log.v(TAG, "setSongToAddToQueue started");
        songToAddToQueue = songID;
        // Log.v(TAG, "setSongToAddToQueue ened");
    }

    private RandomPlaylist playlistToAddToQueue;

    public void setPlaylistToAddToQueue(RandomPlaylist randomPlaylist) {
        // Log.v(TAG, "setPlaylistToAddToQueue started");
        this.playlistToAddToQueue = randomPlaylist;
        // Log.v(TAG, "setPlaylistToAddToQueue ended");
    }

    private boolean fragmentSongVisible = false;

    public void fragmentSongVisible(boolean fragmentSongVisible) {
        // Log.v(TAG, "fragmentSongVisible start");
        this.fragmentSongVisible = fragmentSongVisible;
        // Log.v(TAG, "fragmentSongVisible end");
    }

    public boolean fragmentSongVisible() {
        // Log.v(TAG, "fragmentSongVisible start and end");
        return fragmentSongVisible;
    }

    // region lifecycle

    // region onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log.v(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            loaded = savedInstanceState.getBoolean(KEY_BOOLEAN_LOADED, false);
        }
        viewModelUserPickedPlaylist =
                new ViewModelProvider(this).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(this).get(ViewModelUserPickedSongs.class);
        // Log.v(TAG, "onCreate ended");
    }

    // endregion onCreate

    // region onStart

    @Override
    protected void onStart() {
        // Log.v(TAG, "onStart started");
        onSeekBarChangeListener = new OnSeekBarChangeListener(this);
        setUpActionBar();
        runnableUIUpdate = () -> {
            // Log.v(TAG, "updating UI");
            updateSongUI();
            updateSongPaneUI();
            // Log.v(TAG, "done updating UI");
        };
        super.onStart();
        // Log.v(TAG, "onStart ended");
    }

    private void startAndBindServiceMain() {
        // Log.v(TAG, "starting and binding ServiceMain");
        Intent intentServiceMain = new Intent(getApplicationContext(), ServiceMain.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain);
        } else {
            startService(intentServiceMain);
        }
        connectionServiceMain = new ConnectionServiceMain(this);
        getApplicationContext().bindService(
                intentServiceMain, connectionServiceMain, BIND_AUTO_CREATE | BIND_IMPORTANT);
        // Log.v(TAG, "started and bound ServiceMain");
    }

    public List<Song> getAllSongs() {
        return mediaController.getAllSongs();
    }

    void setUpBroadcastReceivers() {
        // Log.v(TAG, "setting up BroadcastReceivers");
        broadcastReceiverOnCompletion = new BroadcastReceiverOnCompletion(this);
        setUpBroadcastReceiverOnCompletion();
        broadcastReceiverNotificationButtonsForActivityMain =
                new BroadcastReceiverNotificationButtonsForActivityMain(this);
        setUpBroadcastReceiverActionNext();
        setUpBroadcastReceiverActionPrevious();
        setUpBroadcastReceiverActionPlayPause();
        // Log.v(TAG, "done setting up BroadcastReceivers");
    }

    private void setUpBroadcastReceiverOnCompletion() {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(getResources().getString(
                R.string.broadcast_receiver_action_on_completion));
        registerReceiver(broadcastReceiverOnCompletion, filterComplete);
    }

    private void setUpBroadcastReceiverActionNext() {
        IntentFilter filterNext = new IntentFilter();
        filterNext.addAction(getResources().getString(R.string.broadcast_receiver_action_next));
        filterNext.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationButtonsForActivityMain, filterNext);
    }

    private void setUpBroadcastReceiverActionPrevious() {
        IntentFilter filterPrevious = new IntentFilter();
        filterPrevious.addAction(getResources().getString(
                R.string.broadcast_receiver_action_previous));
        filterPrevious.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationButtonsForActivityMain, filterPrevious);
    }

    private void setUpBroadcastReceiverActionPlayPause() {
        IntentFilter filterPlayPause = new IntentFilter();
        filterPlayPause.addAction(getResources().getString(
                R.string.broadcast_receiver_action_play_pause));
        filterPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationButtonsForActivityMain, filterPlayPause);
    }

    void setUpSongPane() {
        // Log.v(TAG, "setting up song pane");
        hideSongPane();
        linkSongPaneButtons();
        setUpDestinationChangedListenerForPaneSong();
        // Log.v(TAG, "done setting up song pane");
    }

    void hideSongPane() {
        // Log.v(TAG, "sending runnable to hide song pane");
        final View fragmentPaneSong = findViewById(R.id.fragmentSongPane);
        if (fragmentPaneSong.getVisibility() != View.INVISIBLE) {
            runOnUiThread(() -> {
                // Log.v(TAG, "hiding song pane");
                fragmentPaneSong.setVisibility(View.INVISIBLE);
                ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.constraintMain, ConstraintSet.BOTTOM);
                constraintSet.applyTo(constraintLayout);
                // Log.v(TAG, "done hiding song pane");
            });
        }
        // Log.v(TAG, "done sending runnable to hide song pane");
    }

    public void showSongPane() {
        // Log.v(TAG, "sending runnable to show song pane");
        final View fragmentPaneSong = findViewById(R.id.fragmentSongPane);
        if (fragmentPaneSong.getVisibility() != View.VISIBLE) {
            runOnUiThread(() -> {
                // Log.v(TAG, "showing song pane");
                findViewById(R.id.fragmentSongPane).setVisibility(View.VISIBLE);
                ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.fragmentSongPane, ConstraintSet.TOP);
                constraintSet.applyTo(constraintLayout);
                updateUI();
                // Log.v(TAG, "done showing song pane");
            });
        }
        // Log.v(TAG, "done sending runnable to show song pane");
    }

    private void linkSongPaneButtons() {
        // Log.v(TAG, "linking song pane buttons");
        onClickListenerSongPane = new OnClickListenerSongPane(this);
        findViewById(R.id.imageButtonSongPaneNext).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageButtonSongPanePlayPause).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageButtonSongPanePrev).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.textViewSongPaneSongName).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageViewSongPaneSongArt).setOnClickListener(onClickListenerSongPane);
        // Log.v(TAG, "done linking song pane buttons");
    }

    private void setUpDestinationChangedListenerForPaneSong() {
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong started");
        onDestinationChangedListenerPanes = new OnDestinationChangedListenerPanes(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerPanes);
        }
        // Log.v(TAG, "setUpDestinationChangedListenerForPaneSong ended");
    }

    private void setUpActionBar() {
        // Log.v(TAG, "Setting up ActionBar");
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(
                    getResources().getColor(R.color.colorOnPrimary), PorterDuff.Mode.SRC_ATOP);
        }
        centerActionBarTitleAndSetTextSize();
        setUpDestinationChangedListenerForToolbar();
        // Log.v(TAG, "Done setting up ActionBar");
    }

    private void centerActionBarTitleAndSetTextSize() {
        // Log.v(TAG, "Centering the ActionBar title");
        ArrayList<View> textViews = new ArrayList<>();
        getWindow().getDecorView().findViewsWithText(textViews, getTitle(), View.FIND_VIEWS_WITH_TEXT);
        if (!textViews.isEmpty()) {
            AppCompatTextView appCompatTextView = null;
            for (View v : textViews) {
                if (v.getParent() instanceof Toolbar) {
                    appCompatTextView = (AppCompatTextView) v;
                    break;
                }
            }
            if (appCompatTextView != null) {
                ViewGroup.LayoutParams params = appCompatTextView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                appCompatTextView.setLayoutParams(params);
                appCompatTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                appCompatTextView.setTextSize(28);
            }
        }
        // Log.v(TAG, "Centered the ActionBar title");
    }

    private void setUpDestinationChangedListenerForToolbar() {
        // Log.v(TAG, "setUpDestinationChangedListenerForToolbar started");
        onDestinationChangedListenerToolbar = new OnDestinationChangedListenerToolbar(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
        // Log.v(TAG, "setUpDestinationChangedListenerForToolbar ended");
    }

    // endregion onStart

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(KEY_BOOLEAN_LOADED, loaded);
    }

    @Override
    protected void onStop() {
        // Log.v(TAG, "onStop started");
        super.onStop();
        getApplicationContext().unbindService(connectionServiceMain);
        connectionServiceMain = null;
        serviceDisconnected();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
        onDestinationChangedListenerToolbar = null;
        onSeekBarChangeListener = null;
        runnableUIUpdate = null;
        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(null);
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(null);
        }
        viewModelUserPickedPlaylist = null;
        viewModelUserPickedSongs = null;
        serviceMain = null;
        playlistToAddToQueue = null;
        // Log.v(TAG, "onStop ended");
    }

    public void unregisterReceivers() {
        // Log.v(TAG, "unregisterReceivers started");
        unregisterReceiver(broadcastReceiverOnCompletion);
        broadcastReceiverOnCompletion = null;
        unregisterReceiver(broadcastReceiverNotificationButtonsForActivityMain);
        broadcastReceiverNotificationButtonsForActivityMain = null;
        // Log.v(TAG, "unregisterReceivers ended");
    }

    public void removeListeners() {
        // Log.v(TAG, "removeListeners started");
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).removeOnDestinationChangedListener(
                    onDestinationChangedListenerPanes);
        }
        onDestinationChangedListenerPanes = null;
        findViewById(R.id.imageButtonSongPaneNext).setOnClickListener(null);
        findViewById(R.id.imageButtonSongPanePlayPause).setOnClickListener(null);
        findViewById(R.id.imageButtonSongPanePrev).setOnClickListener(null);
        findViewById(R.id.textViewSongPaneSongName).setOnClickListener(null);
        findViewById(R.id.imageViewSongPaneSongArt).setOnClickListener(null);
        onClickListenerSongPane = null;
        // Log.v(TAG, "removeListeners ended");
    }

    @Override
    protected void onResume() {
        // Log.v(TAG, "onResume started");
        super.onResume();
        if (loaded) {
            startAndBindServiceMain();
        }
        // Log.v(TAG, "onResume ended");
    }

    @Override
    protected void onPause() {
        // Log.v(TAG, "onPause started");
        super.onPause();
        if (serviceMain != null) {
            serviceMain.shutDownSeekBarUpdater();
        }
        // Log.v(TAG, "onPause ended");
    }


    // endregion lifecycle

    // region UI

    public void setActionBarTitle(String title) {
        // Log.v(TAG, "setting ActionBar title");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
        // Log.v(TAG, "done setting ActionBar title");
    }

    public void showFab(boolean show) {
        // Log.v(TAG, "showing or hiding FAB");
        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        if (show) {
            // Log.v(TAG, "showing FAB");
            fab.show();
        } else {
            // Log.v(TAG, "hiding FAB");
            fab.hide();
        }
        // Log.v(TAG, "done showing or hiding FAB");
    }

    public void setFABText(int id) {
        // Log.v(TAG, "setFABText start");
        ExtendedFloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setText(id);
        // Log.v(TAG, "setFABText end");
    }

    public void setFabImage(int id) {
        // Log.v(TAG, "setting FAB image");
        ExtendedFloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setIcon(ResourcesCompat.getDrawable(getResources(), id, null));
        // Log.v(TAG, "done setting FAB image");
    }

    public void setFabOnClickListener(final View.OnClickListener onClickListener) {
        // Log.v(TAG, "setting FAB OnClickListener");
        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(null);
        fab.setOnClickListener(onClickListener);
        // Log.v(TAG, "done setting FAB OnClickListener");
    }

    public void updateUI() {
        // Log.v(TAG, "sending Runnable to update UI");
        if (runnableUIUpdate != null) {
            runOnUiThread(runnableUIUpdate);
        }
        // Log.v(TAG, "Done sending Runnable to update UI");
    }

    // region updateSongUI

    private void updateSongUI() {
        // Log.v(TAG, "updateSongUI start");
        if (fragmentSongVisible() && mediaController != null && mediaController.getCurrentAudioUri() != null) {
            // Log.v(TAG, "updating SongUI");
            updateSongArt();
            updateSongName();
            updateTextViewTimes();
        } else {
            // Log.v(TAG, "Not updating SongUI");
        }
        updateSeekBar();
        // Log.v(TAG, "updateSongUI end");
    }

    private void updateSeekBar() {
        // Log.v(TAG, "updateSeekBar start");
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null) {
            AudioUri audioUri = mediaController.getCurrentAudioUri();
            final int maxMillis;
            if (audioUri != null) {
                maxMillis = audioUri.getDuration(getApplicationContext());
            } else {
                maxMillis = 9999;
            }
            seekBar.setMax(maxMillis);
            seekBar.setProgress(getCurrentTime());
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
            setUpSeekBarUpdater();
        }
        // Log.v(TAG, "updateSeekBar end");
    }

    private int getCurrentTime() {
        // Log.v(TAG, "getCurrentTime start");
        // Log.v(TAG, "getCurrentTime end");
        return mediaController.getCurrentTime();
    }

    private void setUpSeekBarUpdater() {
        // Log.v(TAG, "setUpSeekBarUpdater start");
        SeekBar seekBar = findViewById(R.id.seekBar);
        TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
        if (seekBar != null && textViewCurrent != null) {
            if (serviceMain != null) {
                serviceMain.updateSeekBarUpdater(seekBar, textViewCurrent);
            }
        }
        // Log.v(TAG, "setUpSeekBarUpdater end");
    }

    private void updateSongArt() {
        // Log.v(TAG, "updateSongArt start");
        final ImageView imageViewSongArt = findViewById(R.id.image_view_song_art);
        if (imageViewSongArt != null) {
            imageViewSongArt.post(runnableSongArtUpdater);
        }
        // Log.v(TAG, "updateSongArt end");
    }

    private void updateSongName() {
        // Log.v(TAG, "updateSongName start");
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        if (textViewSongName != null) {
            textViewSongName.setText(mediaController.getCurrentAudioUri().title);
        }
        // Log.v(TAG, "updateSongName end");
    }

    private void updateTextViewTimes() {
        // Log.v(TAG, "updateTextViewTimes start");
        final int maxMillis = mediaController.getCurrentAudioUri().getDuration(getApplicationContext());
        final String stringEndTime = formatMillis(maxMillis);
        String stringCurrentTime = formatMillis(getCurrentTime());
        TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
        if (textViewCurrent != null) {
            textViewCurrent.setText(stringCurrentTime);
        }
        TextView textViewEnd = findViewById(R.id.editTextEndTime);
        if (textViewEnd != null) {
            textViewEnd.setText(stringEndTime);
        }
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

    // endregion updateSongUI

    // region updateSongPaneUI

    private void updateSongPaneUI() {
        // Log.v(TAG, "updateSongPaneUI start");
        if (!fragmentSongVisible() && (mediaController != null) && mediaController.songInProgress()
                && mediaController.getCurrentAudioUri() != null && mediaController.songInProgress()) {
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
        TextView textViewSongPaneSongName = findViewById(R.id.textViewSongPaneSongName);
        if (textViewSongPaneSongName != null) {
            textViewSongPaneSongName.setText(mediaController.getCurrentAudioUri().title);
        }
        // Log.v(TAG, "updateSongPaneName end");
    }

    private void updateSongPaneArt() {
        // Log.v(TAG, "updateSongPaneArt start");
        final ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null && serviceMain != null) {
            imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater);
        }
        // Log.v(TAG, "updateSongPaneArt end");
    }

    // endregion updateSongPaneUI

    private void updatePlayButtons() {
        // Log.v(TAG, "updatePlayButtons start");
        updateSongPlayButton();
        updateSongPanePlayButton();
        // Log.v(TAG, "updatePlayButtons end");
    }

    private void updateSongPlayButton() {
        // Log.v(TAG, "updateSongPlayButton start");
        ImageButton imageButtonPlayPause = findViewById(R.id.imageButtonPlayPause);
        if (fragmentSongVisible() && imageButtonPlayPause != null) {
            // Log.v(TAG, "updating SongPlayButton");
            if (mediaController.isPlaying()) {
                imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.pause_black_24dp, null));
            } else {
                imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.play_arrow_black_24dp, null));
            }
        } else {
            // Log.v(TAG, "not updating SongPlayButton");
        }
        // Log.v(TAG, "updateSongPlayButton end");
    }

    private void updateSongPanePlayButton() {
        // Log.v(TAG, "updateSongPanePlayButton start");
        ImageButton imageButtonSongPanePlayPause = findViewById(R.id.imageButtonSongPanePlayPause);
        View fragmentPaneSong = findViewById(R.id.fragmentSongPane);
        if ((mediaController != null) && mediaController.songInProgress() && !fragmentSongVisible() &&
                fragmentPaneSong.getVisibility() == View.VISIBLE &&
                imageButtonSongPanePlayPause != null) {
            // Log.v(TAG, "updating SongPanePlayButton");
            if (mediaController.isPlaying()) {
                imageButtonSongPanePlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.pause_black_24dp, null));
            } else {
                imageButtonSongPanePlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.play_arrow_black_24dp, null));
            }
        } else {
            // Log.v(TAG, "Not updating SongPanePlayButton");
        }
        // Log.v(TAG, "updateSongPanePlayButton end");
    }

    public void hideKeyboard(View view) {
        // Log.v(TAG, "hideKeyboard start");
        InputMethodManager imm = (InputMethodManager)
                getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        // Log.v(TAG, "hideKeyboard end");
    }

    public void showToast(int idMessage) {
        // Log.v(TAG, "showToast start");
        Toast toast = Toast.makeText(getApplicationContext(), idMessage, Toast.LENGTH_LONG);
        toast.getView().getBackground().setColorFilter(
                getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        TextView text = toast.getView().findViewById(android.R.id.message);
        text.setTextSize(16);
        toast.show();
        // Log.v(TAG, "showToast end");
    }

    // endregion UI

    // region playbackControls

    public void addToQueue(Long songID) {
        // Log.v(TAG, "addToQueue start");
        mediaController.addToQueue(songID);
        updateUI();
        // Log.v(TAG, "addToQueue end");
    }

    public void addToQueueAndPlay(Long songID) {
        // Log.v(TAG, "addToQueueAndPlay start");
        mediaController.addToQueueAndPlay(serviceMain.getApplicationContext(), songID);
        updateUI();
        // Log.v(TAG, "addToQueueAndPlay end");
    }

    public void playNext() {
        // Log.v(TAG, "playNext start");
        getCurrentPlaylist().bad(
                serviceMain.getApplicationContext(),
                MediaData.getInstance().getSong(mediaController.getCurrentAudioUri().id),
                mediaController.getPercentChangeDown());
        mediaController.playNext(serviceMain.getApplicationContext());
        updateUI();
        // Log.v(TAG, "playNext end");
    }

    public void playPrevious() {
        // Log.v(TAG, "playPrevious start");
        mediaController.playPrevious(serviceMain.getApplicationContext());
        updateUI();
        // Log.v(TAG, "playPrevious end");
    }

    public void pauseOrPlay() {
        // Log.v(TAG, "pauseOrPlay start");
        if (mediaController.getCurrentAudioUri() != null) {
            mediaController.pauseOrPlay(serviceMain.getApplicationContext());
        }
        updatePlayButtons();
        // Log.v(TAG, "pauseOrPlay end");
    }

    public void seekTo(int progress) {
        // Log.v(TAG, "seekTo start");
        mediaController.seekTo(serviceMain.getApplicationContext(), progress);
        // Log.v(TAG, "seekTo end");
    }

    // endregion playbackControls

    public void navigateTo(final int id) {
        // Log.v(TAG, "navigateTo start");
        runOnUiThread(() -> {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            if (fragment != null) {
                NavController navController = NavHostFragment.findNavController(fragment);
                navController.navigate(id);
            }
        });
        // Log.v(TAG, "navigateTo end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Log.v(TAG, "onCreateOptionsMenu start");
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        sendBroadcastOnOptionsMenuCreated();
        // Log.v(TAG, "onCreateOptionsMenu end");
        return true;
    }

    private void sendBroadcastOnOptionsMenuCreated() {
        // Log.v(TAG, "sendBroadcastOnOptionsMenuCreated start");
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        sendBroadcast(intent);
        // Log.v(TAG, "sendBroadcastOnOptionsMenuCreated end");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Log.v(TAG, "onOptionsItemSelected start");
        if (item.getItemId() == R.id.action_reset_probs) {
            mediaController.clearProbabilities(serviceMain.getApplicationContext());
            return true;
        } else if (item.getItemId() == R.id.action_add_to_playlist) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                    new DialogFragmentAddToPlaylist();
            dialogFragmentAddToPlaylist.setArguments(loadBundleForAddToPlaylist(isSong));
            dialogFragmentAddToPlaylist.show(fragmentManager, fragment.getTag());
            return true;
        } else if (item.getItemId() == R.id.action_add_to_queue) {
            if (mediaController.songInProgress() && songToAddToQueue != null) {
                mediaController.addToQueue(songToAddToQueue);

            } else if (playlistToAddToQueue != null) {
                for (Song songs : playlistToAddToQueue.getSongs()) {
                    mediaController.addToQueue(songs.id);
                }
                if (mediaController.songQueueIsEmpty()) {
                    mediaController.setCurrentPlaylist(playlistToAddToQueue);
                }
                if (!mediaController.songInProgress()) {
                    showSongPane();
                }
            }
            if (!mediaController.songInProgress()) {
                mediaController.playNext(serviceMain.getApplicationContext());
                updateUI();
            }
        }
        // Log.v(TAG, "onOptionsItemSelected action_unknown end");
        return super.onOptionsItemSelected(item);
    }

    private Bundle loadBundleForAddToPlaylist(boolean isSong) {
        // Log.v(TAG, "loadBundleForAddToPlaylist start");
        Bundle bundle = new Bundle();
        bundle.putBoolean(BUNDLE_KEY_IS_SONG, isSong);
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, mediaController.getCurrentAudioUri());
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST,
                viewModelUserPickedPlaylist.getUserPickedPlaylist());
        // Log.v(TAG, "loadBundleForAddToPlaylist end");
        return bundle;
    }

    public void saveFile() {
        // Log.v(TAG, "saveFile start");
        SaveFile.saveFile(serviceMain.getApplicationContext());
        // Log.v(TAG, "saveFile end");
    }

    public void setSongPaneArtHeight(int songArtHeight) {
        // Log.v(TAG, "setSongPaneArtHeight start");
        if (serviceMain != null) {
            serviceMain.setSongPaneArtHeight(songArtHeight);
        }
        // Log.v(TAG, "setSongPaneArtHeight end");
    }

    public int getSongPaneArtHeight() {
        // Log.v(TAG, "getSongPaneArtHeight start");
        if (serviceMain != null) {
            // Log.v(TAG, "getSongPaneArtHeight end");
            return serviceMain.getSongPaneArtHeight();
        }
        // Log.v(TAG, "getSongPaneArtHeight default end");
        return -1;
    }

    public void setSongPaneArtWidth(int songArtWidth) {
        // Log.v(TAG, "setSongPaneArtWidth start");
        if (serviceMain != null) {
            serviceMain.setSongPaneArtWidth(songArtWidth);
        }
        // Log.v(TAG, "setSongPaneArtWidth end");
    }

    public int getSongPaneArtWidth() {
        // Log.v(TAG, "getSongPaneArtWidth start");
        if (serviceMain != null) {
            // Log.v(TAG, "getSongPaneArtWidth end");
            return serviceMain.getSongPaneArtWidth();
        }
        // Log.v(TAG, "getSongPaneArtWidth default end");
        return -1;
    }

    public void setUserPickedPlaylist(RandomPlaylist randomPlaylist) {
        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
    }

    public void clearUserPickedSongs() {
        viewModelUserPickedSongs.clearUserPickedSongs();
    }

    public void addUserPickedSong(Song song) {
        viewModelUserPickedSongs.addUserPickedSong(song);
    }

    public RandomPlaylist getUserPickedPlaylist() {
        return viewModelUserPickedPlaylist.getUserPickedPlaylist();
    }

    public boolean serviceConnected() {
        return (serviceMain != null);
    }

    public boolean songInProgress() {
        // Log.v(TAG, "songInProgress start");
        // Log.v(TAG, "songInProgress end");
        return (mediaController != null) && mediaController.songInProgress();
    }

    public boolean isPlaying() {
        // Log.v(TAG, "isPlaying start");
        // Log.v(TAG, "isPlaying end");
        return mediaController.isPlaying();
    }

    public boolean songQueueIsEmpty() {
        // Log.v(TAG, "songQueueIsEmpty start");
        // Log.v(TAG, "songQueueIsEmpty end");
        return mediaController.songQueueIsEmpty();
    }

    public void setCurrentPlaylistToMaster() {
        // Log.v(TAG, "setCurrentPlaylistToMaster start");
        mediaController.setCurrentPlaylistToMaster();
        // Log.v(TAG, "setCurrentPlaylistToMaster end");
    }

    public void setCurrentPlaylist(RandomPlaylist userPickedPlaylist) {
        // Log.v(TAG, "setCurrentPlaylist start");
        mediaController.setCurrentPlaylist(userPickedPlaylist);
        // Log.v(TAG, "setCurrentPlaylist end");
    }

    public AudioUri getCurrentAudioUri() {
        // Log.v(TAG, "getCurrentSong start");
        // Log.v(TAG, "getCurrentSong end");
        return mediaController.getCurrentAudioUri();
    }

    public RandomPlaylist getCurrentPlaylist() {
        // Log.v(TAG, "getCurrentPlaylist start");
        // Log.v(TAG, "getCurrentPlaylist end");
        return mediaController.getCurrentPlaylist();
    }

    public boolean shuffling() {
        // Log.v(TAG, "shuffling start");
        // Log.v(TAG, "shuffling end");
        return mediaController.shuffling();
    }

    public void shuffling(boolean shuffling) {
        // Log.v(TAG, "set shuffling start");
        mediaController.shuffling(shuffling);
        // Log.v(TAG, "set shuffling end");
    }

    public boolean loopingOne() {
        // Log.v(TAG, "loopingOne start");
        // Log.v(TAG, "loopingOne end");
        return mediaController.loopingOne();
    }

    public void loopingOne(boolean loopingOne) {
        // Log.v(TAG, "set loopingOne start");
        mediaController.loopingOne(loopingOne);
        // Log.v(TAG, "set loopingOne end");
    }

    public boolean looping() {
        // Log.v(TAG, "looping start");
        // Log.v(TAG, "looping end");
        return mediaController.looping();
    }

    public void looping(boolean looping) {
        // Log.v(TAG, "set looping start");
        mediaController.looping(looping);
        // Log.v(TAG, "set looping end");
    }

    public void clearSongQueue() {
        mediaController.clearSongQueue();
    }

    // region fragmentLoading

    public void fragmentLoadingStarted() {
        startAndBindServiceMain();
    }

    // endregion fragmentLoading

}
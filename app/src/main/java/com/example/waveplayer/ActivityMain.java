package com.example.waveplayer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_PLAYLISTS;
import static com.example.waveplayer.FragmentPaneSong.getResizedBitmap;

public class ActivityMain extends AppCompatActivity {

    // TODO help page
    // TODO check for leaks
    // TODO warn user about resetting probabilities
    // TODO songpanesongart is not displaying the bitmap
    // TODO create a backup file somewhere
    // TODO allow user to create backup
    
    // TODO AFTER RELEASE
    // Open current song in folder as a menu action
    // Setting to not keep playing after queue is done

    static final String DEBUG_TAG = "debug";
    static final String TAG = "ActivityMain";

    private static final int REQUEST_CODE_PERMISSION_READ = 245083964;
    private static final int REQUEST_CODE_PERMISSION_WRITE = 245083965;

    public static final int MENU_ACTION_RESET_PROBS_INDEX = 0;
    public static final int MENU_ACTION_ADD_TO_PLAYLIST_INDEX = 1;
    public static final int MENU_ACTION_SEARCH_INDEX = 2;
    public static final int MENU_ACTION_ADD_TO_QUEUE = 3;

    private ServiceMain serviceMain;

    public void setServiceMain(ServiceMain serviceMain) {
        Log.v(TAG, "setServiceMain started");
        this.serviceMain = serviceMain;
        setUpAfterServiceConnection();
        Log.v(TAG, "setServiceMain ended");
    }

    private void setUpAfterServiceConnection() {
        Log.v(TAG, "setUpAfterServiceConnection started");
        askForExternalStoragePermissionAndFetchMediaFiles();
        setUpBroadcastReceivers();
        setUpSongPane();
        updateUI();
        runnableSongArtUpdater = new RunnableSongArtUpdater(this);
        runnableSongPaneArtUpdater = new RunnableSongPaneArtUpdater(this);
        Log.v(TAG, "setUpAfterServiceConnection ended");
    }

    public void serviceDisconnected() {
        Log.v(TAG, "serviceDisconnected started");
        unregisterReceivers();
        removeListeners();
        runnableSongArtUpdater = null;
        runnableSongPaneArtUpdater = null;
        Log.v(TAG, "serviceDisconnected end");
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

    public final Object lock = new Object();

    private boolean isSong;

    public void isSong(boolean isSong) {
        Log.v(TAG, "isSong started");
        this.isSong = isSong;
        Log.v(TAG, "isSong ended");
    }

    private AudioUri songToAddToQueue;

    public void setSongToAddToQueue(AudioUri audioUri) {
        Log.v(TAG, "setSongToAddToQueue started");
        songToAddToQueue = audioUri;
        Log.v(TAG, "setSongToAddToQueue ened");
    }

    private RandomPlaylist playlistToAddToQueue;

    public void setPlaylistToAddToQueue(RandomPlaylist randomPlaylist) {
        Log.v(TAG, "setPlaylistToAddToQueue started");
        this.playlistToAddToQueue = randomPlaylist;
        Log.v(TAG, "setPlaylistToAddToQueue ended");
    }

    // region lifecycle

    // region onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "onCreate ended");
    }

    // endregion onCreate

    // region onStart

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart started");
        super.onStart();
        startAndBindServiceMain();
        onSeekBarChangeListener = new OnSeekBarChangeListener(this);
        setUpActionBar();
        runnableUIUpdate = new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "updating UI");
                updateSongUI();
                updateSongPaneUI();
                updatePlayButtons();
                Log.v(TAG, "done updating UI");
            }
        };
        Log.v(TAG, "onStart ended");
    }

    private void startAndBindServiceMain() {
        Log.v(TAG, "starting and binding ServiceMain");
        Intent intentServiceMain = new Intent(ActivityMain.this, ServiceMain.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain);
        } else {
            startService(intentServiceMain);
        }
        connectionServiceMain = new ConnectionServiceMain(this);
        getApplicationContext().bindService(
                intentServiceMain, connectionServiceMain, BIND_AUTO_CREATE | BIND_IMPORTANT);
        Log.v(TAG, "started and bound ServiceMain");
    }

    void askForExternalStoragePermissionAndFetchMediaFiles() {
        Log.v(TAG, "Making sure there is external storage permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION_WRITE);
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION_READ);
            } else {
                serviceMain.getAudioFiles();
            }
        } else {
            serviceMain.getAudioFiles();
        }
        Log.v(TAG, "Done making sure there is external storage permission");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult start");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION_READ && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            serviceMain.getAudioFiles();
        } else if (requestCode == REQUEST_CODE_PERMISSION_READ) {
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.permission_needed, Toast.LENGTH_LONG);
            toast.show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_PERMISSION_READ);
            }
        }
        Log.v(TAG, "onRequestPermissionsResult end");
    }

    public List<AudioUri> getAllSongs() {
        Log.v(TAG, "getAllSongs started");
        if (serviceMain != null) {
            Log.v(TAG, "getAllSongs ended");
            return serviceMain.getAllSongs();
        }
        Log.v(TAG, "getAllSongs default end");
        return null;
    }

    void setUpBroadcastReceivers() {
        Log.v(TAG, "setting up BroadcastReceivers");
        broadcastReceiverOnCompletion = new BroadcastReceiverOnCompletion(this);
        setUpBroadcastReceiverOnCompletion();
        broadcastReceiverNotificationButtonsForActivityMain =
                new BroadcastReceiverNotificationButtonsForActivityMain(this);
        setUpBroadcastReceiverActionNext();
        setUpBroadcastReceiverActionPrevious();
        setUpBroadcastReceiverActionPlayPause();
        Log.v(TAG, "done setting up BroadcastReceivers");
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
        Log.v(TAG, "setting up song pane");
        hideSongPane();
        linkSongPaneButtons();
        setUpDestinationChangedListenerForPaneSong();
        Log.v(TAG, "done setting up song pane");
    }

    void hideSongPane() {
        Log.v(TAG, "sending runnable to hide song pane");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "hiding song pane");
                findViewById(R.id.fragmentSongPane).setVisibility(View.INVISIBLE);
                ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.constraintMain, ConstraintSet.BOTTOM);
                constraintSet.applyTo(constraintLayout);
                Log.v(TAG, "done hiding song pane");
            }
        });
        Log.v(TAG, "done sending runnable to hide song pane");
    }

    void showSongPane() {
        Log.v(TAG, "sending runnable to show song pane");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "showing song pane");
                findViewById(R.id.fragmentSongPane).setVisibility(View.VISIBLE);
                ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
                ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.fragmentSongPane, ConstraintSet.TOP);
                constraintSet.applyTo(constraintLayout);
                updateUI();
                Log.v(TAG, "done showing song pane");
            }
        });
        Log.v(TAG, "done sending runnable to show song pane");
    }

    private void linkSongPaneButtons() {
        Log.v(TAG, "linking song pane buttons");
        onClickListenerSongPane = new OnClickListenerSongPane(this);
        findViewById(R.id.imageButtonSongPaneNext).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageButtonSongPanePlayPause).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageButtonSongPanePrev).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.textViewSongPaneSongName).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageViewSongPaneSongArt).setOnClickListener(onClickListenerSongPane);
        Log.v(TAG, "done linking song pane buttons");
    }

    private void setUpDestinationChangedListenerForPaneSong() {
        Log.v(TAG, "setUpDestinationChangedListenerForPaneSong started");
        onDestinationChangedListenerPanes = new OnDestinationChangedListenerPanes(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerPanes);
        }
        Log.v(TAG, "setUpDestinationChangedListenerForPaneSong ended");
    }

    private void setUpActionBar() {
        Log.v(TAG, "Setting up ActionBar");
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
        Log.v(TAG, "Done setting up ActionBar");
    }

    private void centerActionBarTitleAndSetTextSize() {
        Log.v(TAG, "Centering the ActionBar title");
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
        Log.v(TAG, "Centered the ActionBar title");
    }

    private void setUpDestinationChangedListenerForToolbar() {
        Log.v(TAG, "setUpDestinationChangedListenerForToolbar started");
        onDestinationChangedListenerToolbar = new OnDestinationChangedListenerToolbar(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
        Log.v(TAG, "setUpDestinationChangedListenerForToolbar ended");
    }

    // endregion onStart

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop started");
        super.onStop();
        getApplicationContext().unbindService(connectionServiceMain);
        connectionServiceMain = null;
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
        Log.v(TAG, "onStop ended");
    }

    public void unregisterReceivers() {
        Log.v(TAG, "unregisterReceivers started");
        unregisterReceiver(broadcastReceiverOnCompletion);
        broadcastReceiverOnCompletion = null;
        unregisterReceiver(broadcastReceiverNotificationButtonsForActivityMain);
        broadcastReceiverNotificationButtonsForActivityMain = null;
        Log.v(TAG, "unregisterReceivers ended");
    }

    public void removeListeners() {
        Log.v(TAG, "removeListeners started");
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
        Log.v(TAG, "removeListeners ended");
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume started");
        super.onResume();
        updateUI();
        Log.v(TAG, "onResume ended");
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause started");
        super.onPause();
        if (serviceMain != null) {
            serviceMain.saveFile();
            serviceMain.shutDownSeekBarUpdater();
        }
        Log.v(TAG, "onPause ended");
    }

    // endregion lifecycle

    // region UI

    public void setActionBarTitle(String title) {
        Log.v(TAG, "setting ActionBar title");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
        Log.v(TAG, "done setting ActionBar title");
    }

    public void showFab(boolean show) {
        Log.v(TAG, "showing or hiding FAB");
        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        if (show) {
            Log.v(TAG, "showing FAB");
            fab.show();
        } else {
            Log.v(TAG, "hiding FAB");
            fab.hide();
        }
        Log.v(TAG, "done showing or hiding FAB");
    }

    public void setFABText(int id) {
        Log.v(TAG, "setFABText start");
        ExtendedFloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setText(id);
        Log.v(TAG, "setFABText end");
    }

    public void setFabImage(int id) {
        Log.v(TAG, "setting FAB image");
        ExtendedFloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setIcon(ResourcesCompat.getDrawable(getResources(), id, null));
        Log.v(TAG, "done setting FAB image");
    }

    public void setFabOnClickListener(final View.OnClickListener onClickListener) {
        Log.v(TAG, "setting FAB OnClickListener");
        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(null);
        fab.setOnClickListener(onClickListener);
        Log.v(TAG, "done setting FAB OnClickListener");
    }

    public void updateUI() {
        Log.v(TAG, "sending Runnable to update UI");
        if(runnableUIUpdate != null) {
            runOnUiThread(runnableUIUpdate);
        }
        Log.v(TAG, "Done sending Runnable to update UI");
    }

    // region updateSongUI

    private void updateSongUI() {
        Log.v(TAG, "updateSongUI start");
        if (serviceMain != null
                && serviceMain.fragmentSongVisible() && serviceMain.getCurrentSong() != null) {
            updateSeekBar();
            updateSongArt();
            updateSongName();
            updateTextViewTimes();
        }
        Log.v(TAG, "updateSongUI end");
    }

    private void updateSeekBar() {
        Log.v(TAG, "updateSeekBar start");
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null && serviceMain != null) {
            final int maxMillis = serviceMain.getCurrentSong().getDuration(getApplicationContext());
            seekBar.setMax(maxMillis);
            seekBar.setProgress(getCurrentTime());
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
            setUpSeekBarUpdater(maxMillis);
        }
        Log.v(TAG, "updateSeekBar end");
    }

    private int getCurrentTime() {
        Log.v(TAG, "getCurrentTime start");
        if (serviceMain != null) {
            Log.v(TAG, "getCurrentTime end");
            return serviceMain.getCurrentTime();
        }
        Log.v(TAG, "getCurrentTime default end");
        return -1;
    }

    private void setUpSeekBarUpdater(int maxMillis) {
        Log.v(TAG, "setUpSeekBarUpdater start");
        SeekBar seekBar = findViewById(R.id.seekBar);
        TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
        if (seekBar != null && textViewCurrent != null) {
            if (serviceMain != null) {
                serviceMain.updateSeekBarUpdater(seekBar, textViewCurrent, maxMillis);
            }
        }
        Log.v(TAG, "setUpSeekBarUpdater end");
    }

    private void updateSongArt() {
        Log.v(TAG, "updateSongArt start");
        final ImageView imageViewSongArt = findViewById(R.id.image_view_song_art);
        if (imageViewSongArt != null && serviceMain != null) {
            imageViewSongArt.post(runnableSongArtUpdater);
        }
        Log.v(TAG, "updateSongArt end");
    }

    private void updateSongName() {
        Log.v(TAG, "updateSongName start");
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        if (textViewSongName != null && serviceMain != null) {
            textViewSongName.setText(serviceMain.getCurrentSong().title);
        }
        Log.v(TAG, "updateSongName end");
    }

    private void updateTextViewTimes() {
        Log.v(TAG, "updateTextViewTimes start");
        final int maxMillis = serviceMain.getCurrentSong().getDuration(getApplicationContext());
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
        Log.v(TAG, "updateTextViewTimes end");
    }

    private String formatMillis(int millis) {
        Log.v(TAG, "formatMillis start and end");
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
        Log.v(TAG, "updating the song pane UI");
        if (serviceMain != null
                && !serviceMain.fragmentSongVisible() && serviceMain.getCurrentSong() != null
                && serviceMain.songInProgress()) {
            updateSongPaneName();
            updateSongPaneArt();
        }
        Log.v(TAG, "done updating the song pane UI");
    }

    private void updateSongPaneName() {
        Log.v(TAG, "updateSongPaneName start");
        TextView textViewSongPaneSongName = findViewById(R.id.textViewSongPaneSongName);
        if (textViewSongPaneSongName != null && serviceMain != null) {
            textViewSongPaneSongName.setText(serviceMain.getCurrentSong().title);
        }
        Log.v(TAG, "updateSongPaneName end");
    }

    private void updateSongPaneArt() {
        Log.v(TAG, "updateSongPaneArt start");
        final ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null && serviceMain != null) {
            imageViewSongPaneSongArt.post(runnableSongPaneArtUpdater);
        }
        Log.v(TAG, "updateSongPaneArt end");
    }

    // endregion updateSongPaneUI

    private void updatePlayButtons() {
        Log.v(TAG, "updatePlayButtons start");
        updateSongPlayButton();
        updateSongPanePlayButton();
        Log.v(TAG, "updatePlayButtons end");
    }

    private void updateSongPlayButton() {
        Log.v(TAG, "updateSongPlayButton start");
        ImageButton imageButtonPlayPause = findViewById(R.id.imageButtonPlayPause);
        if (imageButtonPlayPause != null) {
            if (serviceMain != null) {
                if (serviceMain.isPlaying()) {
                    imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                            getResources(), R.drawable.pause_black_24dp, null));
                } else {
                    imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                            getResources(), R.drawable.play_arrow_black_24dp, null));
                }
            }
        }
        Log.v(TAG, "updateSongPlayButton end");
    }

    private void updateSongPanePlayButton() {
        Log.v(TAG, "updateSongPanePlayButton start");
        ImageButton imageButtonSongPanePlayPause = findViewById(R.id.imageButtonSongPanePlayPause);
        if (imageButtonSongPanePlayPause != null) {
            if (serviceMain != null) {
                if (serviceMain.isPlaying()) {
                    imageButtonSongPanePlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                            getResources(), R.drawable.pause_black_24dp, null));
                } else {
                    imageButtonSongPanePlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                            getResources(), R.drawable.play_arrow_black_24dp, null));
                }
            }
        }
        Log.v(TAG, "updateSongPanePlayButton end");
    }

    public void hideKeyboard(View view) {
        Log.v(TAG, "hideKeyboard start");
        InputMethodManager imm = (InputMethodManager)
                getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        Log.v(TAG, "hideKeyboard end");
    }

    public void showToast(int idMessage) {
        Log.v(TAG, "showToast start");
        Toast toast = Toast.makeText(getApplicationContext(), idMessage, Toast.LENGTH_LONG);
        toast.getView().getBackground().setColorFilter(
                getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        TextView text = toast.getView().findViewById(android.R.id.message);
        text.setTextSize(16);
        toast.show();
        Log.v(TAG, "showToast end");
    }

    // endregion UI

    // region playbackControls

    public void addToQueue(Uri uri) {
        Log.v(TAG, "addToQueue start");
        if (serviceMain != null) {
            serviceMain.addToQueue(uri);
        }
        Log.v(TAG, "addToQueue end");
    }

    public void addToQueueAndPlay(AudioUri audioURI) {
        Log.v(TAG, "addToQueueAndPlay start");
        if (serviceMain != null) {
            serviceMain.addToQueueAndPlay(audioURI.getUri());
        }
        updateUI();
        Log.v(TAG, "addToQueueAndPlay end");
    }

    public void playNext() {
        Log.v(TAG, "playNext start");
        if (serviceMain != null) {
            serviceMain.playNext();
        }
        updateUI();
        Log.v(TAG, "playNext end");
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious start");
        if (serviceMain != null) {
            serviceMain.playPrevious();
        }
        updateUI();
        Log.v(TAG, "playPrevious end");
    }

    public void pauseOrPlay() {
        Log.v(TAG, "pauseOrPlay start");
        if (serviceMain != null && serviceMain.getCurrentSong() != null) {
            serviceMain.pauseOrPlay();
        }
        updatePlayButtons();
        Log.v(TAG, "pauseOrPlay end");
    }

    public void seekTo(int progress) {
        Log.v(TAG, "seekTo start");
        if (serviceMain != null) {
            serviceMain.seekTo(progress);
            if (!serviceMain.isPlaying()) {
                pauseOrPlay();
            }
        }
        Log.v(TAG, "seekTo end");
    }

    // endregion playbackControls

    public boolean songInProgress() {
        Log.v(TAG, "songInProgress start");
        if (serviceMain != null) {
            Log.v(TAG, "songInProgress end");
            return serviceMain.songInProgress();
        }
        Log.v(TAG, "songInProgress default end");
        return false;
    }

    public boolean isPlaying() {
        Log.v(TAG, "isPlaying start");
        if (serviceMain != null) {
            Log.v(TAG, "isPlaying end");
            return serviceMain.isPlaying();
        }
        Log.v(TAG, "isPlaying default end");
        return false;
    }

    public boolean songQueueIsEmpty() {
        Log.v(TAG, "songQueueIsEmpty start");
        if (serviceMain != null) {
            Log.v(TAG, "songQueueIsEmpty end");
            return serviceMain.songQueueIsEmpty();
        }
        Log.v(TAG, "songQueueIsEmpty default end");
        return false;
    }

    public ArrayList<RandomPlaylist> getPlaylists() {
        Log.v(TAG, "getPlaylists start");
        if (serviceMain != null) {
            Log.v(TAG, "getPlaylists end");
            return serviceMain.getPlaylists();
        }
        Log.v(TAG, "getPlaylists default end");
        return null;
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        Log.v(TAG, "addPlaylist start");
        if (serviceMain != null) {
            serviceMain.addPlaylist(randomPlaylist);
        }
        Log.v(TAG, "addPlaylist end");
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        Log.v(TAG, "addPlaylist w/ position start");
        if (serviceMain != null) {
            serviceMain.addPlaylist(position, randomPlaylist);
        }
        Log.v(TAG, "addPlaylist w/ position end");
    }

    public void addDirectoryPlaylist(long uriID, RandomPlaylist randomPlaylist) {
        Log.v(TAG, "addDirectoryPlaylist start");
        if (serviceMain != null) {
            serviceMain.addDirectoryPlaylist(uriID, randomPlaylist);
        }
        Log.v(TAG, "addDirectoryPlaylist end");
    }

    public RandomPlaylist getDirectoryPlaylist(long mediaStoreUriID) {
        Log.v(TAG, "getDirectoryPlaylist start");
        if (serviceMain != null) {
            Log.v(TAG, "getDirectoryPlaylist end");
            return serviceMain.getDirectoryPlaylist(mediaStoreUriID);
        }
        Log.v(TAG, "getDirectoryPlaylist default end");
        return null;
    }

    public boolean containsDirectoryPlaylist(long mediaStoreUriID) {
        Log.v(TAG, "containsDirectoryPlaylist start");
        if (serviceMain != null) {
            Log.v(TAG, "containsDirectoryPlaylist end");
            return serviceMain.containsDirectoryPlaylist(mediaStoreUriID);
        }
        Log.v(TAG, "containsDirectoryPlaylist default end");
        return false;
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        Log.v(TAG, "removePlaylist start");
        if (serviceMain != null) {
            serviceMain.removePlaylist(randomPlaylist);
        }
        Log.v(TAG, "removePlaylist end");
    }

    public void setCurrentPlaylistToMaster() {
        Log.v(TAG, "setCurrentPlaylistToMaster start");
        if (serviceMain != null) {
            serviceMain.setCurrentPlaylistToMaster();
        }
        Log.v(TAG, "setCurrentPlaylistToMaster end");
    }

    public void setCurrentPlaylist(RandomPlaylist userPickedPlaylist) {
        Log.v(TAG, "setCurrentPlaylist start");
        if (serviceMain != null) {
            serviceMain.setCurrentPlaylist(userPickedPlaylist);
        }
        Log.v(TAG, "setCurrentPlaylist end");
    }

    public AudioUri getCurrentSong() {
        Log.v(TAG, "getCurrentSong start");
        if (serviceMain != null) {
            Log.v(TAG, "getCurrentSong end");
            return serviceMain.getCurrentSong();
        }
        Log.v(TAG, "getCurrentSong default end");
        return null;
    }

    public RandomPlaylist getCurrentPlaylist() {
        Log.v(TAG, "getCurrentPlaylist start");
        if (serviceMain != null) {
            Log.v(TAG, "getCurrentPlaylist end");
            return serviceMain.getCurrentPlaylist();
        }
        Log.v(TAG, "getCurrentPlaylist default end");
        return null;
    }

    public RandomPlaylist getUserPickedPlaylist() {
        Log.v(TAG, "getUserPickedPlaylist start");
        if (serviceMain != null) {
            Log.v(TAG, "getUserPickedPlaylist end");
            return serviceMain.getUserPickedPlaylist();
        }
        Log.v(TAG, "getUserPickedPlaylist default end");
        return null;
    }

    public void setUserPickedPlaylistToMasterPlaylist() {
        Log.v(TAG, "setUserPickedPlaylistToMasterPlaylist start");
        if (serviceMain != null) {
            serviceMain.setUserPickedPlaylistToMasterPlaylist();
        }
        Log.v(TAG, "setUserPickedPlaylistToMasterPlaylist end");
    }

    public void setUserPickedPlaylist(RandomPlaylist randomPlaylist) {
        Log.v(TAG, "setUserPickedPlaylist start");
        if (serviceMain != null) {
            serviceMain.setUserPickedPlaylist(randomPlaylist);
        }
        Log.v(TAG, "setUserPickedPlaylist end");
    }

    public List<AudioUri> getUserPickedSongs() {
        Log.v(TAG, "getUserPickedSongs start");
        if (serviceMain != null) {
            Log.v(TAG, "getUserPickedSongs end");
            return serviceMain.getUserPickedSongs();
        }
        Log.v(TAG, "getUserPickedSongs default end");
        return null;
    }

    public void addUserPickedSong(AudioUri audioURI) {
        Log.v(TAG, "addUserPickedSong start");
        if (serviceMain != null) {
            serviceMain.addUserPickedSong(audioURI);
        }
        Log.v(TAG, "addUserPickedSong end");
    }

    public void removeUserPickedSong(AudioUri audioURI) {
        Log.v(TAG, "removeUserPickedSong start");
        if (serviceMain != null) {
            serviceMain.removeUserPickedSong(audioURI);
        }
        Log.v(TAG, "removeUserPickedSong end");
    }

    public void clearUserPickedSongs() {
        Log.v(TAG, "clearUserPickedSongs start");
        if (serviceMain != null) {
            serviceMain.clearUserPickedSongs();
        }
        Log.v(TAG, "clearUserPickedSongs end");
    }

    public void fragmentSongVisible(boolean fragmentSongVisible) {
        Log.v(TAG, "fragmentSongVisible start");
        if (serviceMain != null) {
            serviceMain.fragmentSongVisible(fragmentSongVisible);
        }
        Log.v(TAG, "fragmentSongVisible end");
    }

    public void stopAndPreparePrevious() {
        Log.v(TAG, "stopAndPreparePrevious start");
        if (serviceMain != null) {
            serviceMain.stopAndPreparePrevious();
        }
        Log.v(TAG, "stopAndPreparePrevious end");
    }

    public void setMaxPercent(double maxPercent) {
        Log.v(TAG, "setMaxPercent start");
        if (serviceMain != null) {
            serviceMain.setMaxPercent(maxPercent);
        }
        Log.v(TAG, "setMaxPercent end");
    }

    public double getMaxPercent() {
        Log.v(TAG, "getMaxPercent start");
        if (serviceMain != null) {
            Log.v(TAG, "getMaxPercent end");
            return serviceMain.getMaxPercent();
        }
        Log.v(TAG, "getMaxPercent default end");
        return -1;
    }

    public void setPercentChangeUp(double percentChangeUp) {
        Log.v(TAG, "setPercentChangeUp start");
        if (serviceMain != null) {
            serviceMain.setPercentChangeUp(percentChangeUp);
        }
        Log.v(TAG, "setPercentChangeUp end");
    }

    public double getPercentChangeUp() {
        Log.v(TAG, "getPercentChangeUp start");
        if (serviceMain != null) {
            Log.v(TAG, "getPercentChangeUp end");
            return serviceMain.getPercentChangeUp();
        }
        Log.v(TAG, "getPercentChangeUp default end");
        return -1;
    }

    public void setPercentChangeDown(double percentChangeDown) {
        Log.v(TAG, "setPercentChangeDown start");
        if (serviceMain != null) {
            serviceMain.setPercentChangeDown(percentChangeDown);
        }
        Log.v(TAG, "setPercentChangeDown end");
    }

    public double getPercentChangeDown() {
        Log.v(TAG, "getPercentChangeDown start");
        if (serviceMain != null) {
            Log.v(TAG, "getPercentChangeDown end");
            return serviceMain.getPercentChangeDown();
        }
        Log.v(TAG, "getPercentChangeDown default end");
        return -1;
    }

    public boolean shuffling() {
        Log.v(TAG, "shuffling start");
        if (serviceMain != null) {
            Log.v(TAG, "shuffling end");
            return serviceMain.shuffling();
        }
        Log.v(TAG, "shuffling default end");
        return true;
    }

    public void shuffling(boolean shuffling) {
        Log.v(TAG, "set shuffling start");
        if (serviceMain != null) {
            serviceMain.shuffling(shuffling);
        }
        Log.v(TAG, "set shuffling end");
    }

    public boolean loopingOne() {
        Log.v(TAG, "loopingOne start");
        if (serviceMain != null) {
            Log.v(TAG, "loopingOne end");
            return serviceMain.loopingOne();
        }
        Log.v(TAG, "loopingOne default end");
        return false;
    }

    public void loopingOne(boolean loopingOne) {
        Log.v(TAG, "set loopingOne start");
        if (serviceMain != null) {
            serviceMain.loopingOne(loopingOne);
        }
        Log.v(TAG, "set loopingOne end");
    }

    public boolean looping() {
        Log.v(TAG, "looping start");
        if (serviceMain != null) {
            Log.v(TAG, "looping end");
            return serviceMain.looping();
        }
        Log.v(TAG, "loopingOne default end");
        return false;
    }

    public void looping(boolean looping) {
        Log.v(TAG, "set looping start");
        if (serviceMain != null) {
            serviceMain.looping(looping);
        }
        Log.v(TAG, "set looping end");
    }

    public void navigateTo(int id) {
        Log.v(TAG, "navigateTo start");
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            if (fragment != null) {
                NavController navController = NavHostFragment.findNavController(fragment);
                if (navController != null) {
                    navController.navigate(id);
                }
            }
        }
        Log.v(TAG, "navigateTo end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu start");
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        sendBroadcastOnOptionsMenuCreated();
        Log.v(TAG, "onCreateOptionsMenu end");
        return true;
    }

    private void sendBroadcastOnOptionsMenuCreated() {
        Log.v(TAG, "sendBroadcastOnOptionsMenuCreated start");
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        sendBroadcast(intent);
        Log.v(TAG, "sendBroadcastOnOptionsMenuCreated end");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected start");
        if (item.getItemId() == R.id.action_reset_probs && serviceMain != null) {
            serviceMain.clearProbabilities();
            return true;
        } else if (item.getItemId() == R.id.action_add_to_playlist) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist =
                    new DialogFragmentAddToPlaylist();
            dialogFragmentAddToPlaylist.setArguments(loadBundleForAddToPlaylist(isSong));
            dialogFragmentAddToPlaylist.show(fragmentManager, fragment.getTag());
            return true;
        } else if (item.getItemId() == R.id.action_add_to_queue && serviceMain != null) {
            if (serviceMain.songInProgress() && songToAddToQueue != null) {
                serviceMain.addToQueue(songToAddToQueue.getUri());

            } else if (playlistToAddToQueue != null) {
                for (AudioUri audioURI : playlistToAddToQueue.getAudioUris()) {
                    serviceMain.addToQueue(audioURI.getUri());
                }
                if (serviceMain.songQueueIsEmpty()) {
                    serviceMain.setCurrentPlaylist(playlistToAddToQueue);
                }
                if (!serviceMain.songInProgress()) {
                    showSongPane();
                }
            }
            if (!serviceMain.songInProgress()) {
                serviceMain.playNextInQueue(false);
                updateUI();
            }
        }
        Log.v(TAG, "onOptionsItemSelected action_unknown end");
        return super.onOptionsItemSelected(item);
    }

    private Bundle loadBundleForAddToPlaylist(boolean isSong) {
        Log.v(TAG, "loadBundleForAddToPlaylist start");
        Bundle bundle = new Bundle();
        bundle.putBoolean(BUNDLE_KEY_IS_SONG, isSong);
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, serviceMain.getCurrentSong());
        bundle.putSerializable(BUNDLE_KEY_PLAYLISTS, serviceMain.getPlaylists());
        bundle.putSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, serviceMain.getUserPickedPlaylist());
        Log.v(TAG, "loadBundleForAddToPlaylist end");
        return bundle;
    }

    public void saveFile() {
        Log.v(TAG, "saveFile start");
        if (serviceMain != null) {
            serviceMain.saveFile();
        }
        Log.v(TAG, "saveFile end");
    }

    public static Bitmap getThumbnail(AudioUri audioURI, int width, int height, Context context) {
        Log.v(TAG, "getThumbnail start");
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                bitmap = context.getContentResolver().loadThumbnail(
                        audioURI.getUri(), new Size(width, height), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(context.getContentResolver().openFileDescriptor(
                        audioURI.getUri(), "r").getFileDescriptor());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            InputStream inputStream = null;
            if (mmr.getEmbeddedPicture() != null) {
                inputStream = new ByteArrayInputStream(mmr.getEmbeddedPicture());
            }
            mmr.release();
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                return getResizedBitmap(bitmap, width, height);
            }
        }
        Log.v(TAG, "getThumbnail end");
        return bitmap;
    }

    public void setSongPaneArtHeight(int songArtHeight) {
        Log.v(TAG, "setSongPaneArtHeight start");
        if(serviceMain != null){
            serviceMain.setSongPaneArtHeight(songArtHeight);
        }
        Log.v(TAG, "setSongPaneArtHeight end");
    }

    public int getSongPaneArtHeight() {
        Log.v(TAG, "getSongPaneArtHeight start");
        if(serviceMain != null){
            Log.v(TAG, "getSongPaneArtHeight end");
            return serviceMain.getSongPaneArtHeight();
        }
        Log.v(TAG, "getSongPaneArtHeight default end");
        return -1;
    }

    public void setSongPaneArtWidth(int songArtWidth) {
        Log.v(TAG, "setSongPaneArtWidth start");
        if(serviceMain != null){
            serviceMain.setSongPaneArtWidth(songArtWidth);
        }
        Log.v(TAG, "setSongPaneArtWidth end");
    }

    public int getSongPaneArtWidth() {
        Log.v(TAG, "getSongPaneArtWidth start");
        if(serviceMain != null){
            Log.v(TAG, "getSongPaneArtWidth end");
            return serviceMain.getSongPaneArtWidth();
        }
        Log.v(TAG, "getSongPaneArtWidth default end");
        return -1;
    }
    
}
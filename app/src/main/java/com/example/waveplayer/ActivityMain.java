package com.example.waveplayer;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_ADD_TO_PLAYLIST_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_IS_SONG;
import static com.example.waveplayer.DialogFragmentAddToPlaylist.BUNDLE_KEY_PLAYLISTS;

public class ActivityMain extends AppCompatActivity {

    // TODO help page
    // TODO check for leaks
    // TODO warn user about resetting probabilities

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
        this.serviceMain = serviceMain;
    }

    private ServiceConnection connectionServiceMain;

    private BroadcastReceiverOnCompletion broadcastReceiverOnCompletion;
    private BroadcastReceiverNotificationButtonsForActivityMain
            broadcastReceiverNotificationButtonsForActivityMain;

    private OnDestinationChangedListenerToolbar onDestinationChangedListenerToolbar;

    private OnDestinationChangedListenerPanes onDestinationChangedListenerPanes;

    private OnClickListenerSongPane onClickListenerSongPane;

    private OnSeekBarChangeListener onSeekBarChangeListener;

    public final Object lock = new Object();

    private boolean isSong;

    public void isSong(boolean isSong) {
        this.isSong = isSong;
    }

    private AudioUri songToAddToQueue;

    public void setSongToAddToQueue(AudioUri audioUri) {
        songToAddToQueue = audioUri;
    }

    private RandomPlaylist playlistToAddToQueue;

    public void setPlaylistToAddToQueue(RandomPlaylist randomPlaylist) {
        this.playlistToAddToQueue = randomPlaylist;
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
        super.onStart();
        startAndBindServiceMain();
        onSeekBarChangeListener = new OnSeekBarChangeListener(this);
        setUpActionBar();
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
        if (serviceMain != null) {
            return serviceMain.getAllSongs();
        }
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
        onDestinationChangedListenerPanes = new OnDestinationChangedListenerPanes(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerPanes);
        }
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
        Log.v(TAG, "ActionBar set up");
    }

    private void centerActionBarTitleAndSetTextSize() {
        Log.v(TAG, "sending runnable to center the ActionBar title");
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
        onDestinationChangedListenerToolbar = new OnDestinationChangedListenerToolbar(this);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    onDestinationChangedListenerToolbar);
        }
    }

    // endregion onStart

    @Override
    protected void onStop() {
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

        ExtendedFloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(null);
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(null);
        }
    }

    public void unregisterReceivers() {
        unregisterReceiver(broadcastReceiverOnCompletion);
        broadcastReceiverOnCompletion = null;
        unregisterReceiver(broadcastReceiverNotificationButtonsForActivityMain);
        broadcastReceiverNotificationButtonsForActivityMain = null;
    }

    public void removeListeners() {
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
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
            fab.show();
        } else {
            fab.hide();
        }
        Log.v(TAG, "done showing or hiding FAB");
    }

    public void setFABText(int id) {
        ExtendedFloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setText(id);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSongUI();
                updateSongPaneUI();
                updatePlayButtons();
            }
        });
    }

    // region updateSongUI

    private void updateSongUI() {
        Log.v(TAG, "Updating the song UI");
        if (serviceMain != null
                && serviceMain.fragmentSongVisible() && serviceMain.getCurrentSong() != null) {
            updateSeekBar();
            updateSongArt();
            updateSongName();
            updateTextViewTimes();
            Log.v(TAG, "done updating the song UI");
        }
    }

    private void updateSeekBar() {
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null && serviceMain != null) {
            final int maxMillis = serviceMain.getCurrentSong().getDuration(getApplicationContext());
            seekBar.setMax(maxMillis);
            seekBar.setProgress(getCurrentTime());
            seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
            setUpSeekBarUpdater(maxMillis);
        }
    }

    private int getCurrentTime() {
        if (serviceMain != null) {
            return serviceMain.getCurrentTime();
        }
        return -1;
    }

    private void setUpSeekBarUpdater(int maxMillis) {
        SeekBar seekBar = findViewById(R.id.seekBar);
        TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
        if (seekBar != null && textViewCurrent != null) {
            if (serviceMain != null) {
                serviceMain.updateSeekBarUpdater(seekBar, textViewCurrent, maxMillis);
            }
        }
    }

    private void updateSongArt() {
        ImageView imageViewSongArt = findViewById(R.id.image_view_song_art);
        if (imageViewSongArt != null && serviceMain != null) {
            Bitmap bitmap =
                    AudioUri.getThumbnail(serviceMain.getCurrentSong(), getApplicationContext());
            if (bitmap == null) {
                imageViewSongArt.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.music_note_black_48dp, null));
            } else {
                imageViewSongArt.setImageBitmap(bitmap);
            }
        }
    }

    private void updateSongName() {
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        if (textViewSongName != null && serviceMain != null) {
            textViewSongName.setText(serviceMain.getCurrentSong().title);
        }
    }

    private void updateTextViewTimes() {
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
    }

    private String formatMillis(int millis) {
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
        TextView textViewSongPaneSongName = findViewById(R.id.textViewSongPaneSongName);
        if (textViewSongPaneSongName != null && serviceMain != null) {
            textViewSongPaneSongName.setText(serviceMain.getCurrentSong().title);
        }
    }

    private void updateSongPaneArt() {
        int songArtHeight = getSongArtHeight();
        if (songArtHeight > 0) {
            @SuppressWarnings("SuspiciousNameCombination")
            int songArtWidth = songArtHeight;
            setImageViewSongPaneArt(songArtWidth, songArtHeight);
        }
    }

    private int getSongArtHeight() {
        ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null && serviceMain != null) {
            int songArtHeight = imageViewSongPaneSongArt.getMeasuredHeight();
            if (songArtHeight > 0) {
                serviceMain.setSongPaneArtHeight(songArtHeight);
            } else {
                songArtHeight = serviceMain.getSongPaneArtHeight();
            }
            return songArtHeight;
        }
        return -1;
    }

    private void setImageViewSongPaneArt(int songArtWidth, int songArtHeight) {
        ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null && serviceMain != null) {
            Bitmap bitmapSongArt = AudioUri.getThumbnail(
                    serviceMain.getCurrentSong(), getApplicationContext());
            if (bitmapSongArt != null) {
                Bitmap bitmapSongArtResized = FragmentPaneSong.getResizedBitmap(
                        bitmapSongArt, songArtWidth, songArtHeight);
                imageViewSongPaneSongArt.setImageBitmap(bitmapSongArtResized);
            } else {
                Bitmap defaultBitmap = getDefaultBitmap(songArtWidth, songArtHeight);
                if (defaultBitmap != null) {
                    imageViewSongPaneSongArt.setImageBitmap(defaultBitmap);
                }
            }
        }
    }

    private Bitmap getDefaultBitmap(int songArtWidth, int songArtHeight) {
        Drawable drawableSongArt = ResourcesCompat.getDrawable(
                getResources(), R.drawable.music_note_black_48dp, null);
        if (drawableSongArt != null) {
            Bitmap bitmapSongArt;
            drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight);
            bitmapSongArt = Bitmap.createBitmap(
                    songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapSongArt);
            drawableSongArt.draw(canvas);
            Bitmap bitmapSongArtResized = FragmentPaneSong.getResizedBitmap(
                    bitmapSongArt, songArtWidth, songArtHeight);
            bitmapSongArt.recycle();
            return bitmapSongArtResized;
        }
        return null;
    }

    // endregion updateSongPaneUI

    private void updatePlayButtons() {
        Log.v(TAG, "updatePlayButtons start");
        updateSongPlayButton();
        updateSongPanePlayButton();
        Log.v(TAG, "updatePlayButtons end");
    }

    private void updateSongPlayButton() {
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
    }

    private void updateSongPanePlayButton() {
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
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showToast(int idMessage) {
        Toast toast = Toast.makeText(getApplicationContext(), idMessage, Toast.LENGTH_LONG);
        toast.show();
    }

    // endregion UI

    // region playbackControls

    public void addToQueue(Uri uri) {
        if (serviceMain != null) {
            serviceMain.addToQueue(uri);
        }
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
        if (serviceMain != null) {
            serviceMain.seekTo(progress);
            if (!serviceMain.isPlaying()) {
                pauseOrPlay();
            }
        }
    }

    // endregion playbackControls

    public boolean songInProgress() {
        if (serviceMain != null) {
            return serviceMain.songInProgress();
        }
        return false;
    }

    public boolean isPlaying() {
        if (serviceMain != null) {
            return serviceMain.isPlaying();
        }
        return false;
    }

    public boolean songQueueIsEmpty() {
        if (serviceMain != null) {
            return serviceMain.songQueueIsEmpty();
        }
        return false;
    }

    public ArrayList<RandomPlaylist> getPlaylists() {
        if (serviceMain != null) {
            return serviceMain.getPlaylists();
        }
        return null;
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        if (serviceMain != null) {
            serviceMain.addPlaylist(randomPlaylist);
        }
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        if (serviceMain != null) {
            serviceMain.addPlaylist(position, randomPlaylist);
        }
    }

    public void addDirectoryPlaylist(long uriID, RandomPlaylist randomPlaylist) {
        if (serviceMain != null) {
            serviceMain.addDirectoryPlaylist(uriID, randomPlaylist);
        }
    }

    public RandomPlaylist getDirectoryPlaylist(long mediaStoreUriID) {
        if (serviceMain != null) {
            return serviceMain.getDirectoryPlaylist(mediaStoreUriID);
        }
        return null;
    }

    public boolean containsDirectoryPlaylist(long mediaStoreUriID) {
        if (serviceMain != null) {
            return serviceMain.containsDirectoryPlaylist(mediaStoreUriID);
        }
        return false;
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        if (serviceMain != null) {
            serviceMain.removePlaylist(randomPlaylist);
        }
    }

    public void setCurrentPlaylistToMaster() {
        if (serviceMain != null) {
            serviceMain.setCurrentPlaylistToMaster();
        }
    }

    public void setCurrentPlaylist(RandomPlaylist userPickedPlaylist) {
        if (serviceMain != null) {
            serviceMain.setCurrentPlaylist(userPickedPlaylist);
        }
    }

    public AudioUri getCurrentSong() {
        if (serviceMain != null) {
            return serviceMain.getCurrentSong();
        }
        return null;
    }

    public RandomPlaylist getCurrentPlaylist() {
        if (serviceMain != null) {
            return serviceMain.getCurrentPlaylist();
        }
        return null;
    }

    public RandomPlaylist getUserPickedPlaylist() {
        if (serviceMain != null) {
            return serviceMain.getUserPickedPlaylist();
        }
        return null;
    }

    public void setUserPickedPlaylistToMasterPlaylist() {
        if (serviceMain != null) {
            serviceMain.setUserPickedPlaylistToMasterPlaylist();
        }
    }

    public void setUserPickedPlaylist(RandomPlaylist randomPlaylist) {
        if (serviceMain != null) {
            serviceMain.setUserPickedPlaylist(randomPlaylist);
        }
    }

    public List<AudioUri> getUserPickedSongs() {
        if (serviceMain != null) {
            return serviceMain.getUserPickedSongs();
        }
        return null;
    }

    public void addUserPickedSong(AudioUri audioURI) {
        if (serviceMain != null) {
            serviceMain.addUserPickedSong(audioURI);
        }
    }

    public void removeUserPickedSong(AudioUri audioURI) {
        if (serviceMain != null) {
            serviceMain.removeUserPickedSong(audioURI);
        }
    }

    public void clearUserPickedSongs() {
        if (serviceMain != null) {
            serviceMain.clearUserPickedSongs();
        }
    }

    public void fragmentSongVisible(boolean fragmentSongVisible) {
        if (serviceMain != null) {
            serviceMain.fragmentSongVisible(fragmentSongVisible);
        }
    }

    public void stopAndPreparePrevious() {
        if (serviceMain != null) {
            serviceMain.stopAndPreparePrevious();
        }
    }

    public void setMaxPercent(double maxPercent) {
        if (serviceMain != null) {
            serviceMain.setMaxPercent(maxPercent);
        }
    }

    public double getMaxPercent() {
        if (serviceMain != null) {
            return serviceMain.getMaxPercent();
        }
        return -1;
    }

    public void setPercentChangeUp(double percentChangeUp) {
        if (serviceMain != null) {
            serviceMain.setPercentChangeUp(percentChangeUp);
        }
    }

    public double getPercentChangeUp() {
        if (serviceMain != null) {
            return serviceMain.getPercentChangeUp();
        }
        return -1;
    }

    public void setPercentChangeDown(double percentChangeDown) {
        if (serviceMain != null) {
            serviceMain.setPercentChangeDown(percentChangeDown);
        }
    }

    public double getPercentChangeDown() {
        if (serviceMain != null) {
            return serviceMain.getPercentChangeDown();
        }
        return -1;
    }

    public boolean shuffling() {
        if (serviceMain != null) {
            return serviceMain.shuffling();
        }
        return true;
    }

    public void shuffling(boolean shuffling) {
        if (serviceMain != null) {
            serviceMain.shuffling(shuffling);
        }
    }

    public boolean loopingOne() {
        if (serviceMain != null) {
            return serviceMain.loopingOne();
        }
        return false;
    }

    public void loopingOne(boolean loopingOne) {
        if (serviceMain != null) {
            serviceMain.loopingOne(loopingOne);
        }
    }

    public boolean looping() {
        if (serviceMain != null) {
            return serviceMain.looping();
        }
        return false;
    }

    public void looping(boolean looping) {
        if (serviceMain != null) {
            serviceMain.looping(looping);
        }
    }

    public void navigateTo(int id) {
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
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        sendBroadcast(intent);
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
        Bundle bundle = new Bundle();
        bundle.putBoolean(BUNDLE_KEY_IS_SONG, isSong);
        bundle.putSerializable(BUNDLE_KEY_ADD_TO_PLAYLIST_SONG, serviceMain.getCurrentSong());
        bundle.putSerializable(BUNDLE_KEY_PLAYLISTS, serviceMain.getPlaylists());
        bundle.putSerializable(
                BUNDLE_KEY_ADD_TO_PLAYLIST_PLAYLIST, serviceMain.getUserPickedPlaylist());
        return bundle;
    }

    public void saveFile() {
        if (serviceMain != null) {
            serviceMain.saveFile();
        }
    }

    public void clearQueue() {
        if(serviceMain != null){
            serviceMain.clearSongQueue();
        }
    }

}
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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static androidx.annotation.Dimension.SP;
import static com.example.waveplayer.RecyclerViewAdapterSongs.PLAYLISTS;

public class ActivityMain extends AppCompatActivity {

    // TODO update fab with extended FAB
    // TODO help page

    static final String DEBUG_TAG = "debug";
    static final String TAG = "ActivityMain";

    private static final int REQUEST_CODE_PERMISSION_READ = 245083964;

    public static final int MENU_ACTION_RESET_PROBS_INDEX = 0;
    public static final int MENU_ACTION_ADD_TO_PLAYLIST_INDEX = 1;
    public static final int MENU_ACTION_SEARCH_INDEX = 2;
    public static final int MENU_ACTION_ADD_TO_QUEUE = 3;

    public ServiceMain serviceMain;
    final private ServiceConnection connection = new ConnectionServiceMain(this);

    BroadcastReceiverOnCompletion broadcastReceiverOnCompletion =
            new BroadcastReceiverOnCompletion(this);
    BroadcastReceiverNotificationButtonsForActivityMain
            broadcastReceiverNotificationButtonsForActivityMain =
            new BroadcastReceiverNotificationButtonsForActivityMain(this);

    final Object lock = new Object();

    boolean searchInProgress = false;

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
        setUpActionBar();
        startAndBindServiceMain();
    }

    private void setUpActionBar() {
        Log.v(TAG, "Setting up ActionBar");
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(
                    getResources().getColor(R.color.colorOnPrimary), PorterDuff.Mode.SRC_ATOP);
        }
        centerActionBarTitle();
        setSupportActionBar(toolbar);
        setUpDestinationChangedListenerForToolbar();
        Log.v(TAG, "ActionBar set up");
    }

    private void setUpDestinationChangedListenerForToolbar() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    new OnDestinationChangedListenerToolbar(this));
        }
    }

    private void centerActionBarTitle() {
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
                appCompatTextView.setTextSize(18, SP);
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                appCompatTextView.setLayoutParams(params);
                appCompatTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
        }
        Log.v(TAG, "Centered the ActionBar title");
    }

    void setUpBroadcastReceivers() {
        Log.v(TAG, "setting up BroadcastReceivers");
        setUpBroadcastReceiverOnCompletion();
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

    private void startAndBindServiceMain() {
        Log.v(TAG, "starting and binding ServiceMain");
        Intent intentServiceMain = new Intent(ActivityMain.this, ServiceMain.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain);
        } else {
            startService(intentServiceMain);
        }
        getApplicationContext().bindService(
                intentServiceMain, connection, BIND_AUTO_CREATE | BIND_IMPORTANT);
        Log.v(TAG, "started and bound ServiceMain");
    }

    void askForExternalStoragePermissionAndFetchMediaFiles() {
        Log.v(TAG, "Making sure there is external storage permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        OnClickListenerSongPane onClickListenerSongPane = new OnClickListenerSongPane(this);
        findViewById(R.id.imageButtonSongPaneNext).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageButtonSongPanePlayPause).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageButtonSongPanePrev).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.textViewSongPaneSongName).setOnClickListener(onClickListenerSongPane);
        findViewById(R.id.imageViewSongPaneSongArt).setOnClickListener(onClickListenerSongPane);
        Log.v(TAG, "done linking song pane buttons");
    }

    private void setUpDestinationChangedListenerForPaneSong() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    new OnDestinationChangedListenerPanes(this));
        }
    }

    // endregion onStart

    @Override
    protected void onStop() {
        super.onStop();
        getApplicationContext().unbindService(connection);
        unregisterReceiver(broadcastReceiverOnCompletion);
        unregisterReceiver(broadcastReceiverNotificationButtonsForActivityMain);
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
            if(serviceMain.scheduledExecutorService != null) {
                serviceMain.scheduledExecutorService.shutdown();
                serviceMain.scheduledExecutorService = null;
            }
        }
        Log.v(TAG, "onPause ended");
    }

    // endregion lifecycle

    // region UI

    public void setActionBarTitle(final String title) {
                Log.v(TAG, "setting ActionBar title");
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(title);
                }
                Log.v(TAG, "done setting ActionBar title");
    }

    public void setFabImage(final int id) {
                Log.v(TAG, "setting FAB image");
                FloatingActionButton fab;
                fab = findViewById(R.id.fab);
                fab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), id, null));
                Log.v(TAG, "done setting FAB image");
    }

    public void showFab(final boolean show) {
        Log.v(TAG, "showing or hiding FAB");
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        if (show) {
            fab.show();
        } else {
            fab.hide();
        }
        Log.v(TAG, "done showing or hiding FAB");
    }

    public void setFabOnClickListener(final View.OnClickListener onClickListener) {
        Log.v(TAG, "setting FAB OnClickListener");
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(null);
        fab.setOnClickListener(onClickListener);
        Log.v(TAG, "done setting FAB OnClickListener");
    }

    public void updateUI() {
        updateSongUI();
        updateSongPaneUI();
        updatePlayButtons();
        if (serviceMain != null && serviceMain.fragmentSongVisible) {
            hideSearchPane();
        }
    }

    private void showSearchPane() {
        findViewById(R.id.includeSearch).setVisibility(View.VISIBLE);
        ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(R.id.nav_host_fragment, ConstraintSet.TOP,
                R.id.includeSearch, ConstraintSet.BOTTOM);
        constraintSet.applyTo(constraintLayout);
    }

    private void hideSearchPane() {
        findViewById(R.id.includeSearch).setVisibility(View.GONE);
        ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(R.id.nav_host_fragment, ConstraintSet.TOP,
                R.id.constraintMain, ConstraintSet.TOP);
        constraintSet.applyTo(constraintLayout);
    }

    // region updateSongUI

    private void updateSongUI() {
        Log.v(TAG, "Updating the song UI");
        if (serviceMain != null) {
            if (serviceMain.fragmentSongVisible && serviceMain.currentSong != null) {
                updateSeekBar();
                updateSongArt();
                updateSongName();
                updateTextViewTimes();
                Log.v(TAG, "done updating the song UI");
            }
        }
    }

    private void updateSeekBar() {
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null) {
            final int maxMillis = serviceMain.currentSong.getDuration(getApplicationContext());
            seekBar.setProgress(getCurrentTime());
            seekBar.setMax(maxMillis);
            seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(this));
            setUpSeekBarUpdater(maxMillis);
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

    private int getCurrentTime() {
        MediaPlayerWURI mediaPlayerWURI =
                serviceMain.uriMediaPlayerWURIHashMap.get(serviceMain.currentSong.getUri());
        if (mediaPlayerWURI != null) {
            return mediaPlayerWURI.getCurrentPosition();
        } else {
            return 0;
        }
    }

    private void setUpSeekBarUpdater(int maxMillis) {
        SeekBar seekBar = findViewById(R.id.seekBar);
        TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
        if (seekBar != null && textViewCurrent != null) {
            if (serviceMain != null) {
                if(serviceMain.scheduledExecutorService != null) {
                    serviceMain.scheduledExecutorService.shutdown();
                }
                serviceMain.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                serviceMain.scheduledExecutorService.scheduleAtFixedRate(
                        new RunnableSeekBarUpdater(
                                serviceMain.uriMediaPlayerWURIHashMap.get(serviceMain.currentSong.getUri()),
                                seekBar, textViewCurrent, maxMillis,
                                getResources().getConfiguration().locale),
                        0L, 1L, TimeUnit.SECONDS);
            }
        }
    }

    private void updateSongArt() {
        ImageView imageViewSongArt = findViewById(R.id.image_view_song_art);
        if (imageViewSongArt != null) {
            if (AudioURI.getThumbnail(serviceMain.currentSong, getApplicationContext()) == null) {
                imageViewSongArt.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.music_note_black_48dp, null));
            } else {
                imageViewSongArt.setImageBitmap(AudioURI.getThumbnail(
                        serviceMain.currentSong, getApplicationContext()));
            }
        }
    }

    private void updateSongName() {
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        if (textViewSongName != null) {
            textViewSongName.setText(serviceMain.currentSong.title);
        }
    }

    private void updateTextViewTimes() {
        final int maxMillis = serviceMain.currentSong.getDuration(getApplicationContext());
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

    // endregion updateSongUI

    // region updateSongPaneUI

    private void updateSongPaneUI() {
        if (serviceMain != null && !serviceMain.fragmentSongVisible && serviceMain.currentSong != null) {
            Log.v(TAG, "updating the song pane UI");
            if (serviceMain.songInProgress()) {
                updateSongPaneName();
                updateSongPaneArt();
            }
            Log.v(TAG, "done updating the song pane UI");
        }
    }

    private void updateSongPaneName() {
        TextView textViewSongPaneSongName = findViewById(R.id.textViewSongPaneSongName);
        if (textViewSongPaneSongName != null) {
            textViewSongPaneSongName.setText(serviceMain.currentSong.title);
        }
    }

    private void updateSongPaneArt() {
        int songArtHeight = getSongArtHeight();
        if (songArtHeight != -1) {
            @SuppressWarnings("SuspiciousNameCombination")
            int songArtWidth = songArtHeight;
            setImageViewSongPaneArt(songArtWidth, songArtHeight);
        }
    }

    private int getSongArtHeight() {
        ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null) {
            int songArtHeight = imageViewSongPaneSongArt.getMeasuredHeight();
            if (songArtHeight != 0) {
                serviceMain.songPaneArtHeight = songArtHeight;
            } else {
                songArtHeight = serviceMain.songPaneArtHeight;
            }
            return songArtHeight;
        }
        return -1;
    }

    private void setImageViewSongPaneArt(int songArtWidth, int songArtHeight) {
        ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        if (imageViewSongPaneSongArt != null) {
            Bitmap bitmapSongArt = AudioURI.getThumbnail(
                    serviceMain.currentSong, getApplicationContext());
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
            Bitmap bitmapSongArtResized =
                    FragmentPaneSong.getResizedBitmap(
                            bitmapSongArt, songArtWidth, songArtHeight);
            bitmapSongArt.recycle();
            return bitmapSongArtResized;
        }
        return null;
    }

    // endregion updateSongPaneUI

    private void updatePlayButtons() {
        Log.v(TAG, "updatePlayButtons start");
        if (serviceMain != null) {
            serviceMain.updateNotificationPlayButton();
        }
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

    // endregion UI

    // region playbackControls

    public void addToQueueAndPlay(AudioURI audioURI) {
        Log.v(TAG, "addToQueueAndPlay start");
        serviceMain.addToQueueAndPlay(audioURI);
        updateUI();
        Log.v(TAG, "addToQueueAndPlay end");
    }

    public void playNext() {
        Log.v(TAG, "playNext start");
        serviceMain.playNext();
        updateUI();
        Log.v(TAG, "playNext end");
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious start");
        serviceMain.playPrevious();
        updateUI();
        Log.v(TAG, "playPrevious end");
    }

    public void pauseOrPlay() {
        Log.v(TAG, "pauseOrPlay start");
        if (serviceMain.currentSong != null) {
            serviceMain.pauseOrPlay();
        }
        updatePlayButtons();
        Log.v(TAG, "pauseOrPlay end");
    }

    // endregion playbackControls

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
        if (item.getItemId() == R.id.action_reset_probs) {
            serviceMain.currentPlaylist.getProbFun().clearProbs();
            return true;
        } else if (item.getItemId() == R.id.action_add_to_playlist) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
            DialogFragmentAddToPlaylist dialogFragmentAddToPlaylist = new DialogFragmentAddToPlaylist();
            dialogFragmentAddToPlaylist.setArguments(loadBundleForAddToPlaylist());
            dialogFragmentAddToPlaylist.show(fragmentManager, fragment.getTag());
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            showSearchPane();
        } else if (item.getItemId() == R.id.action_add_to_queue) {
            // TODO
            if (serviceMain.songInProgress()) {
                //serviceMain.addToQueue(audioURI.getUri());
            } else {
                /*
                serviceMain.addToQueueAndPlay(audioURI);
                showSongPane();
                updateUI();
                */
            }
        }
        Log.v(TAG, "onOptionsItemSelected action_unknown end");
        return super.onOptionsItemSelected(item);
    }

    private Bundle loadBundleForAddToPlaylist() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(RecyclerViewAdapterSongs.ADD_TO_PLAYLIST_SONG, serviceMain.currentSong);
        bundle.putSerializable(PLAYLISTS, serviceMain.playlists);
        bundle.putSerializable(RecyclerViewAdapterPlaylists.ADD_TO_PLAYLIST_PLAYLIST, serviceMain.userPickedPlaylist);
        return bundle;
    }

}
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

import recyclertreeview.TreeViewAdapter;

public class ActivityMain extends AppCompatActivity {

    // TODO update fab with extended FAB

    // TODO help page

    // TODO file structure

    TreeViewAdapter treeViewAdapter;

    static final String TAG = "ActivityMain";

    private static final int REQUEST_PERMISSION_READ = 245083964;

    private static final int REQUEST_PERMISSION_WRITE = 245083965;

    public static final int MENU_ACTION_RESET_PROBS_INDEX = 0;

    public ServiceMain serviceMain;

    final Object lock = new Object();

    final private ServiceConnection connection = new ConnectionServiceMain(this);

    BroadcastReceiverOnCompletion broadcastReceiverOnCompletion = new BroadcastReceiverOnCompletion(this);

    BroadcastReceiverNotificationForActivityMainMediaControls broadcastReceiverNotificationForActivityMainButtons = new BroadcastReceiverNotificationForActivityMainMediaControls(this);

    // region onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpActionBar();
        startAndBindServiceMain();
        Log.v(TAG, "onCreate ended");
    }


    private void setUpActionBar() {
        Log.v(TAG, "setting up ActionBar");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toolbar toolbar = findViewById(R.id.toolbar);
                toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
                Drawable overflowIcon = toolbar.getOverflowIcon();
                if (overflowIcon != null) {
                    overflowIcon.setColorFilter(getResources().getColor(R.color.colorOnPrimary), PorterDuff.Mode.SRC_ATOP);
                }
                setSupportActionBar(toolbar);
            }
        });
        centerActionBarTitle();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    new OnDestinationChangedListenerToolbar(this));
        }
        Log.v(TAG, "ActionBar set up");
    }

    private void centerActionBarTitle() {
        Log.v(TAG, "sending runnable to center the ActionBar title");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "centering the ActionBar title");
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
                    }
                }
                Log.v(TAG, "done centering the ActionBar title");
            }
        });
        Log.v(TAG, "done sending runnable to center the ActionBar title");
    }


    private void startAndBindServiceMain() {
        Log.v(TAG, "starting and binding ServiceMain");
        Intent intentServiceMain = new Intent(ActivityMain.this, ServiceMain.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain);
        } else {
            startService(intentServiceMain);
        }
        getApplicationContext().bindService(intentServiceMain, connection, BIND_AUTO_CREATE | BIND_IMPORTANT);
        Log.v(TAG, "started and bound ServiceMain");
    }

    void askForExternalStoragePermissionAndFetchMediaFiles() {
        Log.v(TAG, "asking for external storage permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ);
            } else {
                if (!serviceMain.audioFilesLoaded) {
                    serviceMain.getAudioFiles();
                }
            }
        } else {
            if (!serviceMain.audioFilesLoaded) {
                serviceMain.getAudioFiles();
            }
        }
        Log.v(TAG, "asked for external storage permission");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult start");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            serviceMain.getAudioFiles();
        } else if(requestCode == REQUEST_PERMISSION_READ) {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.permission_needed, Toast.LENGTH_LONG);
            toast.show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_READ);
            }
        }
        Log.v(TAG, "onRequestPermissionsResult end");
    }

    void setUpSongPane() {
        Log.v(TAG, "setting up song pane");
        hideSongPane();
        linkSongPaneButtons();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    new OnDestinationChangedListenerSongPane(this));
        }
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

    private void setUpBroadcastReceivers() {
        Log.v(TAG, "setting up BroadcastReceivers");
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(getResources().getString(R.string.broadcast_receiver_action_on_completion));
        registerReceiver(broadcastReceiverOnCompletion, filterComplete);
        IntentFilter filterNext = new IntentFilter();
        filterNext.addAction(getResources().getString(R.string.broadcast_receiver_action_next));
        filterNext.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationForActivityMainButtons, filterNext);
        IntentFilter filterPrevious = new IntentFilter();
        filterPrevious.addAction(getResources().getString(R.string.broadcast_receiver_action_previous));
        filterPrevious.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationForActivityMainButtons, filterPrevious);
        IntentFilter filterPlayPause = new IntentFilter();
        filterPlayPause.addAction(getResources().getString(R.string.broadcast_receiver_action_play_pause));
        filterPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiverNotificationForActivityMainButtons, filterPlayPause);
        Log.v(TAG, "done setting up BroadcastReceivers");

    }

    // endregion onCreate


    @Override
    protected void onResume() {
        super.onResume();
        setUpBroadcastReceivers();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause started");
        super.onPause();
        if (serviceMain != null) {
            serviceMain.saveFile();
            //serviceMain.fragmentSongVisible = false;
            serviceMain.scheduledExecutorService.shutdown();
        }
        unregisterReceiver(broadcastReceiverOnCompletion);
        unregisterReceiver(broadcastReceiverNotificationForActivityMainButtons);
        Log.v(TAG, "onPause ended");
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy started");
        super.onDestroy();
        getApplicationContext().unbindService(connection);
        Log.v(TAG, "onDestroy ended");
    }

    // region publicUI

    public void setActionBarTitle(final String title) {
        Log.v(TAG, "sending Runnable to set ActionBar title");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "setting ActionBar title");
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(title);
                }
                Log.v(TAG, "done setting ActionBar title");
            }
        });
        Log.v(TAG, "done sending Runnable to set ActionBar title");
    }

    public void setFabImage(final int id) {
        Log.v(TAG, "sending Runnable to set FAB image");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "setting FAB image");
                FloatingActionButton fab;
                fab = findViewById(R.id.fab);
                fab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), id, null));
                Log.v(TAG, "done setting FAB image");
            }
        });
        Log.v(TAG, "done sending Runnable to set FAB image");
    }

    public void showFab(final boolean show) {
        Log.v(TAG, "sending Runnable to show or hide FAB");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        });
        Log.v(TAG, "done sending Runnable to show or hide FAB");
    }

    public void setFabOnClickListener(final View.OnClickListener onClickListener) {
        Log.v(TAG, "sending Runnable to set FAB OnClickListener");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "setting FAB OnClickListener");
                FloatingActionButton fab;
                fab = findViewById(R.id.fab);
                fab.setOnClickListener(null);
                fab.setOnClickListener(onClickListener);
                Log.v(TAG, "done setting FAB OnClickListener");
            }
        });
        Log.v(TAG, "done sending Runnable to set FAB OnClickListener");
    }

    public void updateSongUI() {
        Log.v(TAG, "getting ready to update the song UI");
        if(serviceMain != null) {
            if (serviceMain.fragmentSongVisible && serviceMain.currentSong != null) {
                final int millis = serviceMain.currentSong.getDuration(getApplicationContext());
                final String stringEndTime = String.format(getResources().getConfiguration().locale,
                        "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                        TimeUnit.MILLISECONDS.toMinutes(millis) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                        TimeUnit.MILLISECONDS.toSeconds(millis) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
                String stringCurrentTime = "00:00:00";
                SeekBar seekBar = findViewById(R.id.seekBar);
                if (seekBar != null) {
                    final TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
                    seekBar.setMax(millis);
                    MediaPlayerWURI mediaPlayerWURI =
                            serviceMain.songsMap.get(serviceMain.currentSong.getUri());
                    if(mediaPlayerWURI != null) {
                        seekBar.setProgress(mediaPlayerWURI.getCurrentPosition());
                        final int currentMillis = mediaPlayerWURI.getCurrentPosition();
                        stringCurrentTime = String.format(getResources().getConfiguration().locale,
                                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(currentMillis),
                                TimeUnit.MILLISECONDS.toMinutes(currentMillis) -
                                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(currentMillis)),
                                TimeUnit.MILLISECONDS.toSeconds(currentMillis) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentMillis)));
                    }
                    final String finalStringCurrentTime = stringCurrentTime;
                    seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(this));
                    serviceMain.scheduledExecutorService.shutdown();
                    serviceMain.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                    serviceMain.scheduledExecutorService.scheduleAtFixedRate(
                            new RunnableSeekBarUpdater(
                                    serviceMain.songsMap.get(serviceMain.currentSong.getUri()),
                                    seekBar, textViewCurrent, millis, getResources().getConfiguration().locale),
                            0L, 1L, TimeUnit.SECONDS);
                    Log.v(TAG, "sending Runnable to update the song UI");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(TAG, "updating the song UI");
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
                            TextView textViewSongName = findViewById(R.id.text_view_song_name);
                            if (textViewSongName != null) {
                                textViewSongName.setText(serviceMain.currentSong.title);
                            }
                            if (textViewCurrent != null) {
                                textViewCurrent.setText(finalStringCurrentTime);
                            }
                            TextView textViewEnd = findViewById(R.id.editTextEndTime);
                            if (textViewEnd != null) {
                                textViewEnd.setText(stringEndTime);
                            }
                            updatePlayButtons();
                            Log.v(TAG, "done updating the song UI");
                        }
                    });
                    Log.v(TAG, "done sending Runnable to update the song UI");
                } else {
                    Log.v(TAG, "song UI not updated due to not being visible");
                }
            }
        }
        Log.v(TAG, "done getting ready to update the song UI");
    }

    public void updateSongPaneUI() {
        Log.v(TAG, "getting ready to update the song pane UI");
        if (serviceMain != null && !serviceMain.fragmentSongVisible && serviceMain.currentSong != null) {
            Log.v(TAG, "sending Runnable to update the song pane UI");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "updating the song pane UI");
                    if (serviceMain.songInProgress()) {
                        TextView textViewSongPaneSongName = findViewById(R.id.textViewSongPaneSongName);
                        if (textViewSongPaneSongName != null) {
                            textViewSongPaneSongName.setText(serviceMain.currentSong.title);
                        }
                        ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
                        int songArtHeight = imageViewSongPaneSongArt.getMeasuredHeight();
                        if (songArtHeight != 0) {
                            serviceMain.songPaneArtHeight = songArtHeight;
                        } else {
                            songArtHeight = serviceMain.songPaneArtHeight;
                        }
                        @SuppressWarnings("SuspiciousNameCombination") int songArtWidth = songArtHeight;
                        Bitmap bitmapSongArt = AudioURI.getThumbnail(
                                serviceMain.currentSong, getApplicationContext());
                        if (bitmapSongArt != null) {
                            Bitmap bitmapSongArtResized = FragmentSongPane.getResizedBitmap(
                                    bitmapSongArt, songArtWidth, songArtHeight);
                            imageViewSongPaneSongArt.setImageBitmap(bitmapSongArtResized);
                        } else {
                            Drawable drawableSongArt = ResourcesCompat.getDrawable(
                                    getResources(), R.drawable.music_note_black_48dp, null);
                            if (drawableSongArt != null) {
                                drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight);
                                bitmapSongArt = Bitmap.createBitmap(songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
                                Canvas canvas = new Canvas(bitmapSongArt);
                                drawableSongArt.draw(canvas);
                                Bitmap bitmapSongArtResized = FragmentSongPane.getResizedBitmap(bitmapSongArt, songArtWidth, songArtHeight);
                                bitmapSongArt.recycle();
                                imageViewSongPaneSongArt.setImageBitmap(bitmapSongArtResized);
                            }
                        }
                    }
                    Log.v(TAG, "done updating the song pane UI");
                }
            });
            Log.v(TAG,"done sending Runnable to update the song pane UI");
        } else {
                Log.v(TAG, "song pane UI not updated due to not being visible");
            }
        Log.v(TAG, "done getting ready to update the song pane UI");
    }

    // endregion publicUI

    // region playbackControls

    public void addToQueueAndPlay(AudioURI audioURI) {
        Log.v(TAG, "addToQueueAndPlay start");
        serviceMain.addToQueueAndPlay(audioURI);
        updateSongUI();
        updateSongPaneUI();
        Log.v(TAG, "addToQueueAndPlay end");
    }

    public void playNext() {
        Log.v(TAG, "playNext start");
        serviceMain.playNext();
        updatePlayButtons();
        updateSongUI();
        updateSongPaneUI();
        Log.v(TAG, "playNext end");
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious start");
        serviceMain.playPrevious();
        updatePlayButtons();
        updateSongUI();
        updateSongPaneUI();
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

    private void updatePlayButtons() {
        Log.v(TAG, "updatePlayButtons start");
        serviceMain.updateNotificationPlayButton();
        ImageButton imageButtonPlayPause = findViewById(R.id.imageButtonPlayPause);
        ImageButton imageButtonSongPanePlayPause = findViewById(R.id.imageButtonSongPanePlayPause);
        if (imageButtonPlayPause != null) {
            if (serviceMain.isPlaying()) {
                imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.pause_black_24dp, null));
            } else {
                imageButtonPlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.play_arrow_black_24dp, null));
            }
        }
        if (imageButtonSongPanePlayPause != null) {
            if (serviceMain.isPlaying()) {
                imageButtonSongPanePlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.pause_black_24dp, null));
            } else {
                imageButtonSongPanePlayPause.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.play_arrow_black_24dp, null));
            }
        }
        Log.v(TAG, "updatePlayButtons end");
    }

    // endregion playbackControls

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu start");
        getMenuInflater().inflate(R.menu.reset_probs, menu);
        Log.v(TAG, "onCreateOptionsMenu end");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected start");
        switch (item.getItemId()) {
            case R.id.action_reset_probs:
                serviceMain.currentPlaylist.getProbFun().clearProbs();
                Log.v(TAG, "onOptionsItemSelected action_reset_probs end");
                return true;
        }
        Log.v(TAG, "onOptionsItemSelected action_unknown end");
        return super.onOptionsItemSelected(item);
    }

}
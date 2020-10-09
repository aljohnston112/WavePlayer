package com.example.waveplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ActivityMain extends AppCompatActivity {

    // TODO update fab with exteded FAB

    private static final int REQUEST_PERMISSION = 245083964;

    //--------------------------------

    ServiceMain serviceMain;
    boolean serviceMainBound = false;

    // region onCreate

    BroadcastReceiverOnCompletion broadcastReceiverOnCompletion;

    Fragment startFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpActionBar();
        Intent intentServiceMain = new Intent(ActivityMain.this, ServiceMain.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intentServiceMain);
        } else {
            startService(intentServiceMain);
        }
        getApplicationContext().bindService(intentServiceMain, connection, BIND_AUTO_CREATE | BIND_IMPORTANT);
        broadcastReceiverOnCompletion = new BroadcastReceiverOnCompletion(this);
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction("Complete");
        registerReceiver(broadcastReceiverOnCompletion, filterComplete);

        IntentFilter filterNext = new IntentFilter();
        filterNext.addAction("Next");
        filterNext.addCategory(Intent.CATEGORY_DEFAULT);
        BroadcastReceiverNotification actionBroadcastReceiver = new BroadcastReceiverNotification(this);
        registerReceiver(actionBroadcastReceiver, filterNext);

        IntentFilter filterPrev = new IntentFilter();
        filterPrev.addAction("Previous");
        filterPrev.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(actionBroadcastReceiver, filterPrev);

        IntentFilter filterPlayPause = new IntentFilter();
        filterPlayPause.addAction("PlayPause");
        filterPlayPause.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(actionBroadcastReceiver, filterPlayPause);
    }

    @Override
    protected void onPause() {
        super.onPause();
        serviceMain.saveFile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(broadcastReceiverOnCompletion);
        getApplicationContext().unbindService(connection);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ServiceMain.ServiceMainBinder binder = (ServiceMain.ServiceMainBinder) service;
            serviceMain = binder.getService();
            serviceMainBound = true;
            if(!serviceMain.filesFetched) {
                getExternalStoragePermissionAndFetchMediaFiles();
                serviceMain.filesFetched = true;
            }
            createUI();
            if (serviceMain.currentSong != null) {
                updateSongUI();
                updateSongPaneUI();
            }
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setAction("ServiceConnected");
            sendBroadcast(intent);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceMainBound = false;
        }
    };

    private void getExternalStoragePermissionAndFetchMediaFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                serviceMain.getAudioFiles();
            }
        } else {
            serviceMain.getAudioFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            serviceMain.getAudioFiles();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.permission_needed, Toast.LENGTH_LONG);
            toast.show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }
    }

    private void createUI() {
        setUpSongPane();
    }

    private void setUpActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(getResources().getColor(R.color.colorOnPrimary), PorterDuff.Mode.SRC_ATOP);
        }
        setSupportActionBar(toolbar);
        centerActionBarTitle();
    }

    private void centerActionBarTitle() {
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
    }

    private void setUpSongPane() {
        hideSongPane();
        linkSongPaneButtons();
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
        if (fragment != null) {
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(
                    new NavController.OnDestinationChangedListener() {
                        @Override
                        public void onDestinationChanged(@NonNull NavController controller,
                                                         @NonNull NavDestination destination,
                                                         @Nullable Bundle arguments) {
                            if (destination.getId() != R.id.fragmentSong && serviceMain.isPlaying()) {
                                showSongPane();
                            } else {
                                hideSongPane();
                            }
                            Toolbar toolbar = findViewById(R.id.toolbar);
                            Menu menu = toolbar.getMenu();
                            if (menu.size() > 0) {
                                if (destination.getId() == R.id.fragmentPlaylist || destination.getId() == R.id.fragmentSongs) {
                                    menu.getItem(0).setVisible(true);
                                } else {
                                    menu.getItem(0).setVisible(false);
                                }
                            }
                        }
                    });
        }
    }

    public void hideSongPane() {
        findViewById(R.id.fragmentSongPane).setVisibility(View.INVISIBLE);
        ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.constraintMain, ConstraintSet.BOTTOM);
        constraintSet.applyTo(constraintLayout);
    }

    public void showSongPane() {
        findViewById(R.id.fragmentSongPane).setVisibility(View.VISIBLE);
        ConstraintLayout constraintLayout = findViewById(R.id.constraintMain);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);
        constraintSet.connect(R.id.fab, ConstraintSet.BOTTOM, R.id.fragmentSongPane, ConstraintSet.TOP);
        constraintSet.applyTo(constraintLayout);
    }

    private void linkSongPaneButtons() {
        findViewById(R.id.imageButtonSongPaneNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext();
            }
        });
        findViewById(R.id.imageButtonSongPanePlayPause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseOrPlay();
            }
        });
        findViewById(R.id.imageButtonSongPanePrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPrevious();
            }
        });
        findViewById(R.id.textViewSongPaneSongName).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
                if (fragment != null) {
                    NavHostFragment.findNavController(fragment).navigate(R.id.fragmentSong);
                }
            }
        });
        findViewById(R.id.imageViewSongPaneSongArt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment fragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);
                if (fragment != null) {
                    NavHostFragment.findNavController(fragment).navigate(R.id.fragmentSong);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void setActionBarTitle(String string) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(string);
        }
    }

    public void setFabImage(int id) {
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setImageDrawable(getResources().getDrawable(id));
    }

    public void showFab(boolean show) {
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        if (show) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    public void setFabOnClickListener(View.OnClickListener onClickListener) {
        FloatingActionButton fab;
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(null);
        fab.setOnClickListener(onClickListener);
    }

    public void updateSongUI() {
        // TODO move to onCreate when saving data is implemented
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(getApplicationContext(), serviceMain.currentSong.getUri());
        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (time == null) {
            time = "00:00:00";
        }
        int millis = Integer.parseInt(time);
        mediaMetadataRetriever.release();
        //-------------------------------------------------------

        ImageView imageViewSongArt = findViewById(R.id.image_view_song_art);
        if (imageViewSongArt != null) {
            if (serviceMain.currentSong.thumbnail == null) {
                imageViewSongArt.setImageDrawable(getResources().getDrawable((R.drawable.music_note_black_48dp)));
            } else {
                imageViewSongArt.setImageBitmap(serviceMain.currentSong.thumbnail);
            }
        }
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        if (textViewSongName != null) {
            textViewSongName.setText(serviceMain.currentSong.title);
        }
        String stringEndTime = String.format(getResources().getConfiguration().locale,
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        TextView textViewCurrent = findViewById(R.id.editTextCurrentTime);
        if (textViewCurrent != null) {
            textViewCurrent.setText(R.string.start_time);
        }
        TextView textViewEnd = findViewById(R.id.editTextEndTime);
        if (textViewEnd != null) {
            textViewEnd.setText(stringEndTime);
        }
        SeekBar seekBar = findViewById(R.id.seekBar);
        if (seekBar != null) {
            seekBar.setMax(millis);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    MediaPlayerWURI mediaPlayerWURI = serviceMain.songsMap.get(serviceMain.currentSong.getUri());
                    if (mediaPlayerWURI != null) {
                        mediaPlayerWURI.seekTo(seekBar.getProgress());
                    }
                }
            });
            serviceMain.scheduledExecutorService.shutdown();
            serviceMain.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            serviceMain.scheduledExecutorService.scheduleAtFixedRate(
                    new SeekBarUpdater(serviceMain.songsMap.get(serviceMain.currentSong.getUri()), seekBar, textViewCurrent, millis),
                    0L, 1L, TimeUnit.SECONDS);
        }
    }

    class SeekBarUpdater implements Runnable {

        final MediaPlayerWURI mediaPlayerWURI;

        final SeekBar seekBar;

        final int milliSeconds;

        final TextView textViewCurrentTime;

        SeekBarUpdater(MediaPlayerWURI mediaPlayerWURI, SeekBar seekBar, TextView textViewCurrentTime, int milliSeconds) {
            this.mediaPlayerWURI = mediaPlayerWURI;
            this.seekBar = seekBar;
            this.textViewCurrentTime = textViewCurrentTime;
            this.milliSeconds = milliSeconds;
        }

        @Override
        public void run() {
            seekBar.post(new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayerWURI.isPlaying()) {
                        int currentMilliseconds = mediaPlayerWURI.getCurrentPosition();
                        seekBar.setProgress(currentMilliseconds);
                        final String currentTime = String.format(getResources().getConfiguration().locale,
                                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(currentMilliseconds),
                                TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds) -
                                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(currentMilliseconds)),
                                TimeUnit.MILLISECONDS.toSeconds(currentMilliseconds) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentMilliseconds)));
                        textViewCurrentTime.setText(currentTime);
                    }
                }
            });
        }
    }

    public void updateSongPaneUI() {
        TextView textViewSongPaneSongName = findViewById(R.id.textViewSongPaneSongName);
        if (textViewSongPaneSongName != null) {
            textViewSongPaneSongName.setText(serviceMain.currentSong.title);
        }
        ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        int songArtHeight = imageViewSongPaneSongArt.getMeasuredHeight();
        if(songArtHeight != 0){
            serviceMain.songPaneArtHeight = songArtHeight;
        } else{
            songArtHeight = serviceMain.songPaneArtHeight;
        }
        @SuppressWarnings("SuspiciousNameCombination") int songArtWidth = songArtHeight;
        Bitmap bitmapSongArt = serviceMain.currentSong.thumbnail;
        if (bitmapSongArt != null) {
            Bitmap bitmapSongArtResized = FragmentSongPane.getResizedBitmap(bitmapSongArt, songArtWidth, songArtHeight);
            imageViewSongPaneSongArt.setImageBitmap(bitmapSongArtResized);
        } else {
            songArtHeight = imageViewSongPaneSongArt.getMeasuredHeight();
            if(songArtHeight != 0){
                serviceMain.songPaneArtHeight = songArtHeight;
            } else{
                songArtHeight = serviceMain.songPaneArtHeight;
            }
            //noinspection SuspiciousNameCombination
            songArtWidth = songArtHeight;
            Drawable drawableSongArt = getResources().getDrawable(R.drawable.music_note_black_48dp);
            drawableSongArt.setBounds(0, 0, songArtWidth, songArtHeight);
            bitmapSongArt = Bitmap.createBitmap(songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapSongArt);
            drawableSongArt.draw(canvas);
            Bitmap bitmapSongArtResized = FragmentSongPane.getResizedBitmap(bitmapSongArt, songArtWidth, songArtHeight);
            bitmapSongArt.recycle();
            imageViewSongPaneSongArt.setImageBitmap(bitmapSongArtResized);
        }
    }

    public void addToQueueAndPlay(AudioURI audioURI) {
        serviceMain.addToQueueAndPlay(audioURI);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSongUI();
                updateSongPaneUI();
                if (serviceMain != null) {
                    serviceMain.updateNotification();
                }
            }
        });
    }

    public void playNext() {
        serviceMain.playNext();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSongUI();
                updateSongPaneUI();
                serviceMain.updateNotification();
            }
        });
    }

    public void playPrevious() {
        serviceMain.playPrevious();
        if (serviceMain.currentSong != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateSongUI();
                    updateSongPaneUI();
                    if (serviceMain != null) {
                        serviceMain.updateNotification();
                    }
                }
            });
        }
    }

    public void pauseOrPlay() {
        final ImageButton imageButtonPlayPause = findViewById(R.id.imageButtonPlayPause);
        final ImageButton imageButtonSongPanePlayPause = findViewById(R.id.imageButtonSongPanePlayPause);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (imageButtonPlayPause != null) {
                    if (serviceMain.isPlaying()) {
                        imageButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.play_arrow_black_24dp));
                    } else {
                        imageButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_black_24dp));
                    }
                }
                if (imageButtonSongPanePlayPause != null) {
                    if (serviceMain.isPlaying()) {
                        imageButtonSongPanePlayPause.setImageDrawable(getResources().getDrawable(R.drawable.play_arrow_black_24dp));
                    } else {
                        imageButtonSongPanePlayPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_black_24dp));
                    }
                }
            }
        });
        if (serviceMain.currentSong != null) {
            serviceMain.pauseOrPlay();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reset_probs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset_probs:
                serviceMain.userPickedPlaylist.getProbFun().clearProbs();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
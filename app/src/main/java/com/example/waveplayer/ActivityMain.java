package com.example.waveplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityMain extends AppCompatActivity {

    private static final String TAG = "mainActivity";
    private static final int REQUEST_PERMISSION = 245083964;
    private static final String FILE_ERROR_LOG = "error";
    private static final String FILE_SAVE = "playlists";
    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";
    public static final Random random = new Random();

    // Settings
    // TODO UPDATE ALL PLAYLISTS WITH MAX_PERCENT
    static double MAX_PERCENT = 0.1;
    static double PERCENT_CHANGE = 0.5;

    final ArrayList<AudioURI> songs = new ArrayList<>();
    final HashMap<Uri, MediaPlayerWURI> songsMap = new HashMap<>();
    final ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    // TODO make fragments communicate
    final ArrayList<AudioURI> userPickedSongs = new ArrayList<>();
    RandomPlaylist userPickedPlaylist;
    //--------------------------------

    RandomPlaylist masterPlaylist;
    RandomPlaylist currentPlaylist;
    private AudioURI currentSong;
    public AudioURI getCurrentSong(){
        return currentSong;
    }
    final LinkedList<Uri> songQueue = new LinkedList<>();
    ListIterator<Uri> songQueueIterator;

    private boolean isPlaying = false;
    public boolean isPlaying(){
        return isPlaying;
    }

    private boolean haveAudioFocus = false;

    // For updating the SeekBar
     ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            scheduledExecutorService.shutdown();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
        }
    };

    // region onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpCrashSaver();
        logLastCrash();
        getExternalStoragePermissionAndFetchMediaFiles();
        createUI();
        loadSave();
    }

    private void setUpCrashSaver() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread paramThread, @NonNull Throwable paramThrowable) {
                File file = new File(getBaseContext().getFilesDir(), FILE_ERROR_LOG);
                try (PrintWriter pw = new PrintWriter(file)) {
                    paramThrowable.printStackTrace(pw);
                    pw.flush();
                    System.exit(2);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void logLastCrash() {
        File file = new File(getBaseContext().getFilesDir(), FILE_ERROR_LOG);
        if (file.exists()) {
            try (BufferedReader bufferedReader  = new BufferedReader(new FileReader(file))) {
                StringBuilder stringBuilder = new StringBuilder();
                String sCurrentLine;
                while ((sCurrentLine = bufferedReader.readLine()) != null) {
                    stringBuilder.append(sCurrentLine);
                }
                Log.e(TAG, stringBuilder.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadSave() {
        File file = new File(getBaseContext().getFilesDir(), FILE_SAVE);
        if (file.exists()) {
            try (FileInputStream fileInputStream = getApplicationContext().openFileInput("playlists");
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                MAX_PERCENT = objectInputStream.readDouble();
                PERCENT_CHANGE = objectInputStream.readDouble();
                masterPlaylist = (RandomPlaylist) objectInputStream.readObject();
                int playlistSize = objectInputStream.readInt();
                for (int i = 0; i < playlistSize; i++) {
                    playlists.add((RandomPlaylist) objectInputStream.readObject());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void getExternalStoragePermissionAndFetchMediaFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                getAudioFiles();
            }
        } else {
            getAudioFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getAudioFiles();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.permission_needed, Toast.LENGTH_LONG);
            toast.show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }
    }

    private void getAudioFiles() {
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Images.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC +
                " != ?";
        String[] selectionArgs = new String[]{
                "0"
        };
        String sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC";
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String displayName = cursor.getString(nameCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String data = cursor.getString(dataCol);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    Bitmap thumbnail;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        thumbnail = getApplicationContext().getContentResolver().loadThumbnail(
                                uri, new Size(640, 480), null);
                    } else {
                        final int thumbNailWidthAndHeight = 128;
                        thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(data),
                                thumbNailWidthAndHeight, thumbNailWidthAndHeight);
                    }
                    AudioURI audioURI = new AudioURI(uri, thumbnail, displayName, artist, title, id);
                    songs.add(audioURI);
                }
                if (!songs.isEmpty()) {
                    // TODO load master playlist from file
                    masterPlaylist = new RandomPlaylist(songs, MAX_PERCENT, MASTER_PLAYLIST_NAME);
                    currentPlaylist = masterPlaylist;
                    songQueueIterator = songQueue.listIterator();
                }
            }
        } catch (IOException e) {
            Toast toast = Toast.makeText(getApplicationContext(), R.string.thumbnail_error, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void createUI() {
        setContentView(R.layout.activity_main);
        setUpActionBar();
        setUpSongPane();
    }

    private void setUpActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorOnPrimary));
        toolbar.getOverflowIcon().setColorFilter(getResources().getColor(R.color.colorOnPrimary), PorterDuff.Mode.SRC_ATOP);
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
            NavHostFragment.findNavController(fragment).addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                @Override
                public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                    if (destination.getId() != R.id.fragmentSong && isPlaying) {
                        showSongPane();
                    } else {
                        hideSongPane();
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

    // endregion onCreate

// region onStop

    @Override
    protected void onStop() {
        super.onStop();
        saveFile();
    }

    private void saveFile() {
        File file = new File(getBaseContext().getFilesDir(), FILE_SAVE);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        try (FileOutputStream fos = getApplicationContext().openFileOutput("playlists", Context.MODE_PRIVATE);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
            objectOutputStream.writeDouble(MAX_PERCENT);
            objectOutputStream.writeDouble(PERCENT_CHANGE);
            objectOutputStream.writeObject(masterPlaylist);
            objectOutputStream.writeInt(playlists.size());
            for (RandomPlaylist randomPlaylist : playlists) {
                objectOutputStream.writeObject(randomPlaylist);
            }
            objectOutputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


// endregion onStop

    // region ActivityMain UI calls

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
        mediaMetadataRetriever.setDataSource(getApplicationContext(), currentSong.getUri());
        String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if (time == null) {
            time = "00:00:00";
        }
        int millis = Integer.parseInt(time);
        mediaMetadataRetriever.release();
        //-------------------------------------------------------

        ImageView imageViewSongArt = findViewById(R.id.image_view_song_art);
        if (imageViewSongArt != null) {
            if(currentSong.thumbnail == null){
                imageViewSongArt.setImageDrawable(getResources().getDrawable((R.drawable.music_note_black_48dp)));
            } else {
                imageViewSongArt.setImageBitmap(currentSong.thumbnail);
            }
        }
        TextView textViewSongName = findViewById(R.id.text_view_song_name);
        if (textViewSongName != null) {
            textViewSongName.setText(currentSong.title);
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
                    MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
                    if (mediaPlayerWURI != null) {
                        mediaPlayerWURI.seekTo(seekBar.getProgress());
                    }
                }
            });
            scheduledExecutorService.shutdown();
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(
                    new SeekBarUpdater(songsMap.get(currentSong.getUri()), seekBar, textViewCurrent, millis),
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
            textViewSongPaneSongName.setText(currentSong.title);
        }
        ImageView imageViewSongPaneSongArt = findViewById(R.id.imageViewSongPaneSongArt);
        int songArtHeight = imageViewSongPaneSongArt.getMeasuredHeight();
        @SuppressWarnings("SuspiciousNameCombination") int songArtWidth = songArtHeight;
        Bitmap bitmapSongArt = currentSong.thumbnail;
        if (bitmapSongArt != null) {
            Bitmap bitmapSongArtResized = FragmentSongPane.getResizedBitmap(bitmapSongArt, songArtWidth, songArtHeight);
            imageViewSongPaneSongArt.setImageBitmap(bitmapSongArtResized);
        } else {
            songArtHeight = imageViewSongPaneSongArt.getMeasuredHeight();
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

    // endregion ActivityMain UI calls

    // region Music Controls

    public void addToQueueAndPlay(AudioURI audioURI) {
        Log.v(TAG, "addToQueueAndPlay");
        stopAndPreparePrevious();
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(audioURI.getUri());
        if (mediaPlayerWURI == null) {
            mediaPlayerWURI = makeMediaPlayerWURIAndPlay(audioURI);
        } else {
            if(haveAudioFocus) {
                mediaPlayerWURI.shouldStart(true);
                isPlaying = true;
            } else {
                if(requestAudioFocus()){
                    mediaPlayerWURI.shouldStart(true);
                    isPlaying = true;
                }
            }
        }
        songQueueIterator = null;
        songQueue.add(mediaPlayerWURI.audioURI.getUri());
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(mediaPlayerWURI.audioURI.getUri()));
        songQueueIterator.next();
        currentSong = audioURI;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSongUI();
                updateSongPaneUI();
            }
        });
    }

    private void stopAndPreparePrevious() {
        Log.v(TAG, "stopPreviousAndPrepare");
        if (songQueueIterator.hasPrevious()) {
            final MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
            if (mediaPlayerWURI != null) {
                if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                    mediaPlayerWURI.stop();
                    mediaPlayerWURI.prepareAsync();
                    isPlaying = false;
                }
            }
            songQueueIterator.next();
        }
    }

    private MediaPlayerWURI makeMediaPlayerWURIAndPlay(AudioURI audioURI) {
        Log.v(TAG, "makeMediaPlayerWURI");
        MediaPlayerWURI mediaPlayerWURI = new CreateMediaPlayerWURIThread(this, audioURI).call();
        songsMap.put(mediaPlayerWURI.audioURI.getUri(), mediaPlayerWURI);
        if(haveAudioFocus) {
            mediaPlayerWURI.shouldStart(true);
            isPlaying = true;
        } else {
            if(requestAudioFocus()){
                mediaPlayerWURI.shouldStart(true);
                isPlaying = true;
            }
        }
        currentSong = mediaPlayerWURI.audioURI;
        return mediaPlayerWURI;
    }

    public class CreateMediaPlayerWURIThread implements Callable<MediaPlayerWURI> {

        final ActivityMain activityMain;

        final AudioURI audioURI;

        CreateMediaPlayerWURIThread(ActivityMain activityMain, AudioURI audioURI) {
            this.activityMain = activityMain;
            this.audioURI = audioURI;
        }

        @Override
        public MediaPlayerWURI call() {
            MediaPlayerWURI mediaPlayerWURI = new MediaPlayerWURI(
                    activityMain, MediaPlayer.create(getApplicationContext(), audioURI.getUri()), audioURI);
            mediaPlayerWURI.setOnCompletionListener(null);
            mediaPlayerWURI.setOnCompletionListener(onCompletionListener);
            return mediaPlayerWURI;
        }

    }

    void playNext() {
        Log.v(TAG, "playNext");
        currentPlaylist.getProbFun().bad(getCurrentSong(), ActivityMain.PERCENT_CHANGE);
        stopAndPreparePrevious();
        if (songQueueIterator.hasNext()) {
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.next());
            if (mediaPlayerWURI != null) {
                currentSong = mediaPlayerWURI.audioURI;
                if(haveAudioFocus) {
                    mediaPlayerWURI.shouldStart(true);
                    isPlaying = true;
                } else {
                    if(requestAudioFocus()){
                        mediaPlayerWURI.shouldStart(true);
                        isPlaying = true;
                    }
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateSongUI();
                    updateSongPaneUI();
                }
            });
        } else {
            addToQueueAndPlay(currentPlaylist.getProbFun().fun(random));
        }
    }

    public void playPrevious() {
        Log.v(TAG, "playPrevious");
        stopAndPreparePrevious();
        Uri uri = null;
        if (songQueueIterator.hasPrevious()) {
            uri = songQueueIterator.previous();
        }
        if (songQueueIterator.hasPrevious()) {
            MediaPlayerWURI mediaPlayerWURI = songsMap.get(songQueueIterator.previous());
            if (mediaPlayerWURI != null) {
                if(haveAudioFocus) {
                    mediaPlayerWURI.shouldStart(true);
                    isPlaying = true;
                } else {
                    if(requestAudioFocus()){
                        mediaPlayerWURI.shouldStart(true);
                        isPlaying = true;
                    }
                }
                currentSong = mediaPlayerWURI.audioURI;
            }
            songQueueIterator.next();
        } else {
            if (uri != null) {
                MediaPlayerWURI mediaPlayerWURI = songsMap.get(uri);
                if (mediaPlayerWURI != null) {
                    if(haveAudioFocus) {
                        mediaPlayerWURI.shouldStart(true);
                        isPlaying = true;
                    } else {
                        if(requestAudioFocus()){
                            mediaPlayerWURI.shouldStart(true);
                            isPlaying = true;
                        }
                    }
                    currentSong = mediaPlayerWURI.audioURI;
                }
                songQueueIterator.next();
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateSongUI();
                updateSongPaneUI();
            }
        });
    }

    public void pauseOrPlay() {
        Log.v(TAG, "pauseOrPlay");
        MediaPlayerWURI mediaPlayerWURI = songsMap.get(currentSong.getUri());
        final ImageButton imageButtonPlayPause = findViewById(R.id.imageButtonPlayPause);
        final ImageButton imageButtonSongPanePlayPause = findViewById(R.id.imageButtonSongPanePlayPause);
        if (mediaPlayerWURI != null) {
            if (mediaPlayerWURI.isPrepared && mediaPlayerWURI.isPlaying()) {
                mediaPlayerWURI.pause();
                isPlaying = false;
            } else {
                if(haveAudioFocus) {
                    mediaPlayerWURI.shouldStart(true);
                    isPlaying = true;
                } else {
                    if(requestAudioFocus()){
                        mediaPlayerWURI.shouldStart(true);
                        isPlaying = true;
                    }
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (imageButtonPlayPause != null) {
                        if (isPlaying) {
                            imageButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_black_24dp));
                        } else {
                            imageButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.play_arrow_black_24dp));
                        }
                    }
                    if (imageButtonSongPanePlayPause != null) {
                        if (isPlaying) {
                            imageButtonSongPanePlayPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_black_24dp));
                        } else {
                            imageButtonSongPanePlayPause.setImageDrawable(getResources().getDrawable(R.drawable.play_arrow_black_24dp));
                        }
                    }
                }
            });
        }
    }

    // endregion Music Controls

    public void releaseMediaPlayers() {
        synchronized (this) {
            Iterator<MediaPlayerWURI> collection = songsMap.values().iterator();
            MediaPlayerWURI mediaPlayerWURI;
            while (collection.hasNext()) {
                mediaPlayerWURI = collection.next();
                mediaPlayerWURI.release();
                collection.remove();
            }
        }
    }

    private boolean requestAudioFocus() {
        if(haveAudioFocus){
            return true;
        }
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int i) {

                }
            }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            haveAudioFocus = true;
        } else{
            haveAudioFocus = false;
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reset_probs, menu);
        return true;
    }

}
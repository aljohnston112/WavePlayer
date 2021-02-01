package com.example.waveplayer.media_controller;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.MediaStore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.random_playlist.SongDAO;
import com.example.waveplayer.random_playlist.SongDatabase;
import com.example.waveplayer.service_main.ServiceMain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class MediaData {

    protected static final String SONG_DATABASE_NAME = "SONG_DATABASE_NAME";

    private static final Object MEDIA_DATA_LOCK = new Object();

    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

    private static MediaData INSTANCE;

    private final MutableLiveData<String> loadingText = new MutableLiveData<>();

    private final MutableLiveData<Double> loadingProgress = new MutableLiveData<>(0.0);

    private final HashMap<Long, MediaPlayerWUri> songIDToMediaPlayerWUriHashMap = new HashMap<>();

    private final ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    private SongDAO songDAO;

    private RandomPlaylist masterPlaylist;

    private Settings settings =
            new Settings(0.1, 0.1, 0.5, 0);

    /** Returns a singleton instance of this class.
     *  loadData(Context context) must be called for methods on the singleton to function properly.
     * @return A singleton instance of this class that may or may not be loaded with data.
     */
    protected static MediaData getInstance() {
        synchronized (MEDIA_DATA_LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new MediaData();
            }
            return INSTANCE;
        }
    }

    protected LiveData<String> getLoadingText() {
        return loadingText;
    }

    protected LiveData<Double> getLoadingProgress() {
        return loadingProgress;
    }

    protected MediaPlayerWUri getMediaPlayerWUri(Long songID) {
        return songIDToMediaPlayerWUriHashMap.get(songID);
    }

    protected void addMediaPlayerWUri(Long id, MediaPlayerWUri mediaPlayerWURI) {
        songIDToMediaPlayerWUriHashMap.put(id, mediaPlayerWURI);
    }

    protected void releaseMediaPlayers() {
        synchronized (this) {
            Iterator<MediaPlayerWUri> iterator = songIDToMediaPlayerWUriHashMap.values().iterator();
            MediaPlayerWUri mediaPlayerWURI;
            while (iterator.hasNext()) {
                mediaPlayerWURI = iterator.next();
                mediaPlayerWURI.release();
                iterator.remove();
            }
        }
    }

    protected ArrayList<RandomPlaylist> getPlaylists() {
        return playlists;
    }

    protected void addPlaylist(RandomPlaylist randomPlaylist) {
        playlists.add(randomPlaylist);
    }

    protected void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        playlists.add(position, randomPlaylist);
    }

    protected void removePlaylist(RandomPlaylist randomPlaylist) {
        playlists.remove(randomPlaylist);
    }

    protected RandomPlaylist getPlaylist(String playlistName) {
        RandomPlaylist out = null;
        for (RandomPlaylist randomPlaylist : playlists) {
            if (randomPlaylist.getName().equals(playlistName)) {
                out = randomPlaylist;
            }
            break;
        }
        return out;
    }

    protected List<Song> getAllSongs() {
        return masterPlaylist.getSongs();
    }

    protected void setMasterPlaylist(RandomPlaylist masterPlaylist) {
        this.masterPlaylist = masterPlaylist;
    }

    protected RandomPlaylist getMasterPlaylist() {
        return masterPlaylist;
    }

    protected Settings getSettings() {
        return settings;
    }

    protected void setSettings(Settings settings) {
        this.settings = settings;
    }

    protected double getMaxPercent() {
        return settings.maxPercent;
    }

    protected void setMaxPercent(double maxPercent) {
        masterPlaylist.setMaxPercent(maxPercent);
        for (RandomPlaylist randomPlaylist : playlists) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
        settings = new Settings(
                maxPercent, settings.percentChangeUp, settings.percentChangeDown, settings.lowerProb);
    }

    protected void setPercentChangeUp(double percentChangeUp) {
        settings = new Settings(
                settings.maxPercent, percentChangeUp, settings.percentChangeDown, settings.lowerProb);

    }

    protected double getPercentChangeUp() {
        return settings.percentChangeUp;
    }

    protected void setPercentChangeDown(double percentChangeDown) {
        settings = new Settings(
                settings.maxPercent, settings.percentChangeUp, percentChangeDown, settings.lowerProb);
    }

    protected double getPercentChangeDown() {
        return settings.percentChangeDown;
    }

    protected void setLowerProb(double lowerProb) {
        settings = new Settings(
                settings.maxPercent, settings.percentChangeUp, settings.percentChangeDown, lowerProb);
    }

    protected double getLowerProb() {
        return settings.lowerProb;
    }

    protected void loadData(Context context) {
        SongDatabase songDatabase = Room.databaseBuilder(context, SongDatabase.class, SONG_DATABASE_NAME).build();
        songDAO = songDatabase.songDAO();
        SaveFile.loadSaveFile(context, this);
        getSongsFromMediaStore(context);
    }

    private void getSongsFromMediaStore(final Context context) {
        final Resources resources = context.getResources();
        final ArrayList<Song> newSongs = new ArrayList<>();
        final ArrayList<Long> filesThatExist = new ArrayList<>();
        final ExecutorService executorService = ServiceMain.executorServiceFIFO;
        executorService.execute((Runnable) () -> {
            String[] projection = new String[]{
                    MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.TITLE};
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != ?";
            String[] selectionArgs = new String[]{"0"};
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, selection, selectionArgs, sortOrder)) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
                    final int i = cursor.getCount();
                    int j = 0;
                    loadingText.postValue(resources.getString(R.string.loading1));
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String displayName = cursor.getString(nameCol);
                        String title = cursor.getString(titleCol);
                        String artist = cursor.getString(artistCol);
                        AudioUri audioURI = new AudioUri(displayName, artist, title, id);
                        File file = new File(context.getFilesDir(), String.valueOf(audioURI.id));
                        if (!file.exists() || (songDAO.getSong(id) == null)) {
                            try (FileOutputStream fos = context.openFileOutput(String.valueOf(id), Context.MODE_PRIVATE);
                                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
                                objectOutputStream.writeObject(audioURI);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            newSongs.add(new Song(id, title));
                        }
                        filesThatExist.add(id);
                        loadingProgress.postValue((double) ((double) j) / ((double) i));
                        j++;
                    }
                    loadingText.postValue(resources.getString(R.string.loading2));
                }
            }
        });
        executorService.execute((Runnable) () -> {
            int i = newSongs.size();
            int k = 0;
            for (Song song : newSongs) {
                songDAO.insertAll(song);
                loadingProgress.postValue((double) ((double) k) / ((double) i));
                k++;
            }
        });
        executorService.execute(() -> {
            if (masterPlaylist != null) {
                loadingText.postValue(resources.getString(R.string.loading3));
                addNewSongs(filesThatExist);
                loadingText.postValue(resources.getString(R.string.loading4));
                removeMissingSongs(filesThatExist);
            } else {
                masterPlaylist = new RandomPlaylist(
                        MediaData.MASTER_PLAYLIST_NAME, new ArrayList<>(newSongs),
                        settings.maxPercent, true);
            }
        });
        executorService.execute(() -> {
            if (settings.lowerProb == 0) {
                setLowerProb(2.0 / masterPlaylist.size());
            }
            SaveFile.saveFile(context);
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setAction(context.getResources().getString(R.string.broadcast_receiver_action_loaded));
            context.sendBroadcast(intent);
        });
    }

    private void removeMissingSongs(List<Long> filesThatExist) {
        int i = filesThatExist.size();
        int j = 0;
        for (Long songID : masterPlaylist.getSongIDs()) {
            if (!filesThatExist.contains(songID)) {
                masterPlaylist.remove(songDAO.getSong(songID));
                songIDToMediaPlayerWUriHashMap.remove(songID);
                songDAO.delete(songDAO.getSong(songID));
            }
            loadingProgress.postValue((double) ((double) j) / ((double) i));
            j++;
        }
    }

    private void addNewSongs(List<Long> filesThatExist) {
        int i = filesThatExist.size();
        int j = 0;
        for (Long songID : filesThatExist) {
            if (!masterPlaylist.contains(songID)) {
                masterPlaylist.add(songDAO.getSong(songID));
            }
            loadingProgress.postValue((double) ((double) j) / ((double) i));
            j++;
        }
    }

    protected Song getSong(final Long songID) {
        Song song = null;
        try {
            song = ServiceMain.executorServicePool.submit(() -> songDAO.getSong(songID)).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return song;
    }

}
package com.example.waveplayer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import androidx.room.Room;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MediaData {

    private static final String SONG_DATABASE_NAME = "SONG_DATABASE_NAME";

    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

    private final SongDatabase songDatabase;

    private final SongDAO songDAO;

    private final HashMap<Long, MediaPlayerWUri> songIDToMediaPlayerWUriHashMap = new HashMap<>();

    private Settings settings = new Settings(0.1, 0.1, 0.5);

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public Settings getSettings() {
        return settings;
    }

    private RandomPlaylist masterPlaylist;

    public void setMasterPlaylist(RandomPlaylist masterPlaylist) {
        this.masterPlaylist = masterPlaylist;
    }

    public RandomPlaylist getMasterPlaylist() {
        return masterPlaylist;
    }

    public List<Song> getAllSongs() {
        return masterPlaylist.getSongs();
    }

    private final ArrayList<RandomPlaylist> playlists = new ArrayList<>();

    public ArrayList<RandomPlaylist> getPlaylists() {
        return playlists;
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        playlists.add(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        playlists.add(position, randomPlaylist);
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        playlists.remove(randomPlaylist);
    }

    public static MediaData INSTANCE;

    private MediaData(Context context) {
        songDatabase = Room.databaseBuilder(context, SongDatabase.class, SONG_DATABASE_NAME).build();
        songDAO = songDatabase.songDAO();
        SaveFile.loadSaveFile(context, this);
        getAudioFiles(context);
    }

    synchronized public static MediaData getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MediaData(context);
        }
        return INSTANCE;
    }

    public double getMaxPercent() {
        return settings.maxPercent;
    }

    public void setMaxPercent(double maxPercent) {
        masterPlaylist.setMaxPercent(maxPercent);
        for (RandomPlaylist randomPlaylist : playlists) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
        settings = new Settings(maxPercent, settings.percentChangeUp, settings.percentChangeDown);
    }

    public void setPercentChangeUp(double percentChangeUp) {
        settings = new Settings(settings.maxPercent, percentChangeUp, settings.percentChangeDown);

    }

    public double getPercentChangeUp() {
        return settings.percentChangeUp;
    }

    public void setPercentChangeDown(double percentChangeDown) {
        settings = new Settings(settings.maxPercent, settings.percentChangeUp, percentChangeDown);
    }

    public double getPercentChangeDown() {
        return settings.percentChangeDown;
    }

    MediaPlayerWUri getMediaPlayerWUri(Long songID) {
        return songIDToMediaPlayerWUriHashMap.get(songID);
    }

    public void addMediaPlayerWUri(Long id, MediaPlayerWUri mediaPlayerWURI) {
        songIDToMediaPlayerWUriHashMap.put(id, mediaPlayerWURI);
    }

    public void releaseMediaPlayers() {
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

    private void getAudioFiles(Context context) {
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.TITLE};
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ?";
        String[] selectionArgs = new String[]{"0"};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                getSongs(context, cursor);
            }
        }
    }

    private void getSongs(Context context, Cursor cursor) {
        final ArrayList<Song> newSongs = new ArrayList<>();
        final ArrayList<Long> filesThatExist = new ArrayList<>();
        int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
        int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(idCol);
            String displayName = cursor.getString(nameCol);
            String title = cursor.getString(titleCol);
            String artist = cursor.getString(artistCol);
            AudioUri audioURI = new AudioUri(displayName, artist, title, id);
            File file = new File(context.getFilesDir(), String.valueOf(audioURI.id));
            if (!file.exists()) {
                try (FileOutputStream fos =
                             context.openFileOutput(String.valueOf(id), Context.MODE_PRIVATE);
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
        }
        if (masterPlaylist != null) {
            // TODO race condition!
            ServiceMain.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    addNewSongs(filesThatExist);
                    removeMissingSongs(filesThatExist);
                }
            });
        } else {
            masterPlaylist = new RandomPlaylist(
                    MediaData.MASTER_PLAYLIST_NAME, new ArrayList<>(newSongs),
                    settings.maxPercent, true, -1);
            ServiceMain.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    songDAO.insertAll(newSongs.toArray(new Song[0]));
                }
            });
        }
    }

    private void removeMissingSongs(List<Long> filesThatExist) {
        for (Long songID : masterPlaylist.getSongIDs()) {
            if (!filesThatExist.contains(songID)) {
                masterPlaylist.remove(songDAO.getSong(songID));
                songIDToMediaPlayerWUriHashMap.remove(songID);
            }
        }
    }

    private void addNewSongs(List<Long> filesThatExist) {
        for (Long songID : filesThatExist) {
            if (!masterPlaylist.contains(songID)) {
                masterPlaylist.add(songDAO.getSong(songID));
            }
        }
    }

    public Song getSong(Long songID) {
        return songDAO.getSong(songID);
    }

    public static AudioUri getAudioUri(Context context, Long songID) {
        File file = new File(context.getFilesDir(), String.valueOf(songID));
        if (file.exists()) {
            try (FileInputStream fileInputStream = context.openFileInput(String.valueOf(songID));
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                return (AudioUri) objectInputStream.readObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
package com.example.waveplayer.media_controller;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import androidx.room.Room;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.service_main.ServiceMain;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MediaData {

    public static final String SONG_DATABASE_NAME = "SONG_DATABASE_NAME";

    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

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

    public RandomPlaylist getPlaylist(String playlistName) {
        RandomPlaylist out = null;
        for(RandomPlaylist randomPlaylist : playlists){
            if(randomPlaylist.getName().equals(playlistName)){
                out = randomPlaylist;
            }
            break;
        }
        return out;
    }

    public static MediaData INSTANCE;

    private MediaData(Context context) {
        SongDatabase songDatabase = Room.databaseBuilder(context, SongDatabase.class, SONG_DATABASE_NAME).build();
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

    private void getSongs(final Context context, Cursor cursor) {
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
        ServiceMain.executorService.submit(new Runnable() {
            @Override
            public void run() {
                SaveFile.saveFile(context);
            }
        });
    }

    private void removeMissingSongs(List<Long> filesThatExist) {
        for (Long songID : masterPlaylist.getSongIDs()) {
            if (!filesThatExist.contains(songID)) {
                masterPlaylist.remove(songDAO.getSong(songID));
                songIDToMediaPlayerWUriHashMap.remove(songID);
                songDAO.delete(songDAO.getSong(songID));
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

    public Song getSong(final Long songID) {
        Future<Song> songFuture = ServiceMain.executorService.submit(new Callable<Song>() {
            @Override
            public Song call() throws Exception {
                return songDAO.getSong(songID);
            }
        });
        Song out = null;
        try {
            out = songFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return out;
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

    public static Bitmap getThumbnail(AudioUri audioURI, int width, int height, Context context) {
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
        return bitmap;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
    }

}
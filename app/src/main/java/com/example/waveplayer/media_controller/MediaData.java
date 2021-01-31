package com.example.waveplayer.media_controller;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Size;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ActivityMain;
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
import java.util.concurrent.ExecutorService;

public class MediaData {

    private static final Object lock = new Object();

    public static final String SONG_DATABASE_NAME = "SONG_DATABASE_NAME";

    private static final String MASTER_PLAYLIST_NAME = "MASTER_PLAYLIST_NAME";

    private final MutableLiveData<Double> mLoadingProgress = new MutableLiveData<>(0.0);

    public LiveData<Double> getLoadingProgress() {
        return mLoadingProgress;
    }

    private final MutableLiveData<String> mLoadingText = new MutableLiveData<>();

    public LiveData<String> getLoadingText() {
        return mLoadingText;
    }

    private SongDAO songDAO;

    private final HashMap<Long, MediaPlayerWUri> mSongIDToMediaPlayerWUriHashMap = new HashMap<>();

    private Settings mSettings = new Settings(
            0.1, 0.1, 0.5, 0);

    public void setSettings(Settings settings) {
        this.mSettings = settings;
    }

    public Settings getSettings() {
        return mSettings;
    }

    private RandomPlaylist mMasterPlaylist;

    public void setMasterPlaylist(RandomPlaylist masterPlaylist) {
        this.mMasterPlaylist = masterPlaylist;
    }

    public RandomPlaylist getMasterPlaylist() {
        return mMasterPlaylist;
    }

    public List<Song> getAllSongs() {
        return mMasterPlaylist.getSongs();
    }

    private final ArrayList<RandomPlaylist> mPlaylists = new ArrayList<>();

    public ArrayList<RandomPlaylist> getPlaylists() {
        return mPlaylists;
    }

    public void addPlaylist(RandomPlaylist randomPlaylist) {
        mPlaylists.add(randomPlaylist);
    }

    public void addPlaylist(int position, RandomPlaylist randomPlaylist) {
        mPlaylists.add(position, randomPlaylist);
    }

    public void removePlaylist(RandomPlaylist randomPlaylist) {
        mPlaylists.remove(randomPlaylist);
    }

    public RandomPlaylist getPlaylist(String playlistName) {
        RandomPlaylist out = null;
        for(RandomPlaylist randomPlaylist : mPlaylists){
            if(randomPlaylist.getName().equals(playlistName)){
                out = randomPlaylist;
            }
            break;
        }
        return out;
    }

    public static MediaData M_INSTANCE;

    public void loadData(ActivityMain activityMain){
        Context context = activityMain.getApplicationContext();
        SongDatabase songDatabase = Room.databaseBuilder(context, SongDatabase.class, SONG_DATABASE_NAME).build();
        songDAO = songDatabase.songDAO();
        SaveFile.loadSaveFile(context, this);
        getSongs(activityMain);
    }

    public static MediaData getInstance() {
        synchronized(lock) {
            if(M_INSTANCE == null){
                M_INSTANCE = new MediaData();
            }
            return M_INSTANCE;
        }
    }

    public double getMaxPercent() {
        return mSettings.maxPercent;
    }

    public void setMaxPercent(double maxPercent) {
        mMasterPlaylist.setMaxPercent(maxPercent);
        for (RandomPlaylist randomPlaylist : mPlaylists) {
            randomPlaylist.setMaxPercent(maxPercent);
        }
        mSettings = new Settings(
                maxPercent, mSettings.percentChangeUp, mSettings.percentChangeDown, mSettings.lowerProb);
    }

    public void setPercentChangeUp(double percentChangeUp) {
        mSettings = new Settings(
                mSettings.maxPercent, percentChangeUp, mSettings.percentChangeDown, mSettings.lowerProb);

    }

    public double getPercentChangeUp() {
        return mSettings.percentChangeUp;
    }

    public void setPercentChangeDown(double percentChangeDown) {
        mSettings = new Settings(
                mSettings.maxPercent, mSettings.percentChangeUp, percentChangeDown, mSettings.lowerProb);
    }

    public double getPercentChangeDown() {
        return mSettings.percentChangeDown;
    }

    public void setLowerProb(double lowerProb) {
        mSettings = new Settings(
                mSettings.maxPercent, mSettings.percentChangeUp, mSettings.percentChangeDown, lowerProb);
    }

    public double getLowerProb() {
        return mSettings.lowerProb;
    }

    MediaPlayerWUri getMediaPlayerWUri(Long songID) {
        return mSongIDToMediaPlayerWUriHashMap.get(songID);
    }

    public void addMediaPlayerWUri(Long id, MediaPlayerWUri mediaPlayerWURI) {
        mSongIDToMediaPlayerWUriHashMap.put(id, mediaPlayerWURI);
    }

    public void releaseMediaPlayers() {
        synchronized (this) {
            Iterator<MediaPlayerWUri> iterator = mSongIDToMediaPlayerWUriHashMap.values().iterator();
            MediaPlayerWUri mediaPlayerWURI;
            while (iterator.hasNext()) {
                mediaPlayerWURI = iterator.next();
                mediaPlayerWURI.release();
                iterator.remove();
            }
        }
    }

    private void getSongs(final ActivityMain activityMain) {
        final Context context = activityMain.getApplicationContext();
        final Resources resources = activityMain.getResources();
        final ArrayList<Song> newSongs = new ArrayList<>();
        final ArrayList<Long> filesThatExist = new ArrayList<>();
        final ExecutorService executorService = ServiceMain.executorServiceFIFO;
        executorService.execute((Runnable) () -> {
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
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
                    final int i = cursor.getCount();
                    int j = 0;
                    mLoadingText.postValue(resources.getString(R.string.loading1));
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String displayName = cursor.getString(nameCol);
                        String title = cursor.getString(titleCol);
                        String artist = cursor.getString(artistCol);
                        AudioUri audioURI = new AudioUri(displayName, artist, title, id);
                        File file = new File(context.getFilesDir(), String.valueOf(audioURI.id));
                        if (!file.exists()) {
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
                        mLoadingProgress.postValue((double) ((double) j) / ((double) i));
                        j++;
                    }
                    mLoadingText.postValue(resources.getString(R.string.loading2));
                }
            }
        });
        executorService.execute((Runnable) () -> {
            int i = newSongs.size();
            int k = 0;
            for(Song song : newSongs) {
                songDAO.insertAll(song);
                mLoadingProgress.postValue((double)((double)k)/((double)i));
                k++;
            }
        });
        executorService.execute(() -> {
            if (mMasterPlaylist != null) {
                mLoadingText.postValue(resources.getString(R.string.loading3));
                addNewSongs(filesThatExist);
                mLoadingText.postValue(resources.getString(R.string.loading4));
                removeMissingSongs(filesThatExist);
            } else {
                mMasterPlaylist = new RandomPlaylist(
                        MediaData.MASTER_PLAYLIST_NAME, new ArrayList<>(newSongs),
                        mSettings.maxPercent, true);
            }
        });
        executorService.execute(() -> {
            if(mSettings.lowerProb == 0){
                setLowerProb(2.0/mMasterPlaylist.size());
            }
            SaveFile.saveFile(context);
            activityMain.loaded(true);
            activityMain.navigateTo(R.id.FragmentTitle);
        });
    }

    private void removeMissingSongs(List<Long> filesThatExist) {
        int i = filesThatExist.size();
        int j = 0;
        for (Long songID : mMasterPlaylist.getSongIDs()) {
            if (!filesThatExist.contains(songID)) {
                mMasterPlaylist.remove(songDAO.getSong(songID));
                mSongIDToMediaPlayerWUriHashMap.remove(songID);
                songDAO.delete(songDAO.getSong(songID));
            }
            mLoadingProgress.postValue((double)((double)j)/((double)i));
            j++;
        }
    }

    private void addNewSongs(List<Long> filesThatExist) {
        int i = filesThatExist.size();
        int j = 0;
        for (Long songID : filesThatExist) {
            if (!mMasterPlaylist.contains(songID)) {
                mMasterPlaylist.add(songDAO.getSong(songID));
            }
            mLoadingProgress.postValue((double)((double)j)/((double)i));
            j++;
        }
    }

    public Song getSong(final Long songID) {
        Song song = null;
        try {
            song = ServiceMain.executorServicePool.submit(new Callable<Song>() {
                @Override
                public Song call() {
                    return songDAO.getSong(songID);
                }
            }).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return song;
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
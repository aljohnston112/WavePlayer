package com.example.waveplayer.media_controller;

import android.content.Context;

import androidx.room.Room;

import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.SongDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SaveFile {

    private static final String FILE_SAVE = "playlists";

    private static final String FILE_SAVE2 = "playlists2";

    private static final String FILE_SAVE3 = "playlists3";

    private static final long SAVE_FILE_VERIFICATION_NUMBER = 8479145830949658990L;

    public static void loadSaveFile(final Context context, final MediaData mediaData) {
        ServiceMain.executorServiceFIFO.submit(() -> {
            if (attemptLoadFile(context, mediaData, FILE_SAVE)!=(SAVE_FILE_VERIFICATION_NUMBER)) {
                if (attemptLoadFile(context, mediaData, FILE_SAVE2)!=(SAVE_FILE_VERIFICATION_NUMBER)) {
                    if (attemptLoadFile(context, mediaData, FILE_SAVE3)!=(SAVE_FILE_VERIFICATION_NUMBER)) {
                        SongDatabase songDatabase = Room.databaseBuilder(
                                context, SongDatabase.class, MediaData.SONG_DATABASE_NAME).build();
                        songDatabase.songDAO().deleteAll();
                    }
                }
            }
        });
    }

    private static long attemptLoadFile(Context context, MediaData mediaData, String fileSave) {
        long longEOF = 0L;
        File file = new File(context.getFilesDir(), fileSave);
        if (file.exists()) {
            try (FileInputStream fileInputStream = context.openFileInput(fileSave);
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                mediaData.setSettings((Settings) objectInputStream.readObject());
                mediaData.setMasterPlaylist((RandomPlaylist) objectInputStream.readObject());
                int playlistSize = objectInputStream.readInt();
                for (int i = 0; i < playlistSize; i++) {
                    mediaData.addPlaylist((RandomPlaylist) objectInputStream.readObject());
                }
                longEOF = objectInputStream.readLong();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return longEOF;
    }

    public static void saveFile(final Context context) {
        ServiceMain.executorServicePool.submit(() -> {
            MediaData mediaData = MediaData.getInstance();
            File file = new File(context.getFilesDir(), FILE_SAVE);
            File file2 = new File(context.getFilesDir(), FILE_SAVE2);
            File file3 = new File(context.getFilesDir(), FILE_SAVE3);
            if (file3.exists()) {
                file3.delete();
            }
            file2.renameTo(file3);
            file.renameTo(file2);
            File file4 = new File(context.getFilesDir(), FILE_SAVE);
            file4.delete();
            try (FileOutputStream fos = context.openFileOutput(FILE_SAVE, Context.MODE_PRIVATE);
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
                objectOutputStream.writeObject(mediaData.getSettings());
                objectOutputStream.writeObject(mediaData.getMasterPlaylist());
                objectOutputStream.writeInt(mediaData.getPlaylists().size());
                for (RandomPlaylist randomPlaylist : mediaData.getPlaylists()) {
                    objectOutputStream.writeObject(randomPlaylist);
                }
                objectOutputStream.writeLong(SAVE_FILE_VERIFICATION_NUMBER);
                objectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
package com.example.waveplayer;

import android.content.Context;

import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SaveFile {

    private static final String FILE_SAVE = "playlists";

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void saveFile(final Context context) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                MediaData mediaData = MediaData.getInstance(context);
                File file = new File(context.getFilesDir(), FILE_SAVE);
                file.delete();
                try (FileOutputStream fos = context.openFileOutput(FILE_SAVE, Context.MODE_PRIVATE);
                     ObjectOutputStream objectOutputStream = new ObjectOutputStream(fos)) {
                    objectOutputStream.writeObject(mediaData.getSettings());
                    objectOutputStream.writeObject(mediaData.getMasterPlaylist());
                    objectOutputStream.writeInt(mediaData.getPlaylists().size());
                    for (RandomPlaylist randomPlaylist : mediaData.getPlaylists()) {
                        objectOutputStream.writeObject(randomPlaylist);
                    }
                    objectOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static void loadSaveFile(Context context, MediaData mediaData) {
        File file = new File(context.getFilesDir(), FILE_SAVE);
        if (file.exists()) {
            try (FileInputStream fileInputStream = context.openFileInput(FILE_SAVE);
                 ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
                mediaData.setSettings((Settings) objectInputStream.readObject());
                mediaData.setMasterPlaylist((RandomPlaylist) objectInputStream.readObject());
                int playlistSize = objectInputStream.readInt();
                for (int i = 0; i < playlistSize; i++) {
                    mediaData.addPlaylist((RandomPlaylist) objectInputStream.readObject());
                }
            }  catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}

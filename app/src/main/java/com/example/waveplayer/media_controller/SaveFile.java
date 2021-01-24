package com.example.waveplayer.media_controller;

import android.content.Context;
import android.widget.Toast;

import androidx.room.Room;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.service_main.ServiceMain;

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

    private static final Long SAVE_FILE_VERIFICATION_NUMBER = 8479145830949658990L;

    public static void saveFile(final Context context) {
        ServiceMain.executorService.submit(new Runnable() {
            @Override
            public void run() {
                MediaData mediaData = MediaData.getInstance(context);
                File file = new File(context.getFilesDir(), FILE_SAVE);
                File file2 = new File(context.getFilesDir(), FILE_SAVE2);
                File file3 = new File(context.getFilesDir(), FILE_SAVE3);
                if(file3.exists()){
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
            }
        });
    }


    public static void loadSaveFile(final Context context, MediaData mediaData) {
        if(!attemptLoadFile(context, mediaData, FILE_SAVE).equals(SAVE_FILE_VERIFICATION_NUMBER)){
            if(!attemptLoadFile(context, mediaData, FILE_SAVE2).equals(SAVE_FILE_VERIFICATION_NUMBER)){
                if(!attemptLoadFile(context, mediaData, FILE_SAVE3).equals(SAVE_FILE_VERIFICATION_NUMBER)){
                    // TODO
                    ServiceMain.executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            SongDatabase songDatabase = Room.databaseBuilder(context, SongDatabase.class, MediaData.SONG_DATABASE_NAME).build();
                            songDatabase.songDAO().deleteAll();
                        }
                    });
                }
            }
        }
    }

    private static Long attemptLoadFile(Context context, MediaData mediaData, String fileSave) {
        Long longEOF = 0L;
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
            }  catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return longEOF;
    }

}

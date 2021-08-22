package com.fourthFinger.pinkyPlayer.media_controller

import android.content.Context
import androidx.room.Room
import com.fourthFinger.pinkyPlayer.random_playlist.RandomPlaylist
import com.fourthFinger.pinkyPlayer.random_playlist.SettingsRepo
import com.fourthFinger.pinkyPlayer.random_playlist.SongDatabase
import java.io.*

object SaveFile {

    private const val FILE_SAVE: String = "playlists"
    private const val FILE_SAVE2: String = "playlists2"
    private const val FILE_SAVE3: String = "playlists3"
    private const val SAVE_FILE_VERIFICATION_NUMBER = 8479145830949658990L

    private val settingsRepo = SettingsRepo.getInstance()

    fun loadSaveFile(context: Context, mediaData: MediaData) {
        ServiceMain.executorServiceFIFO.submit {
            if (attemptLoadFile(context, mediaData, FILE_SAVE) != SAVE_FILE_VERIFICATION_NUMBER) {
                if (attemptLoadFile(context, mediaData, FILE_SAVE2) != SAVE_FILE_VERIFICATION_NUMBER) {
                    if (attemptLoadFile(context, mediaData, FILE_SAVE3) != SAVE_FILE_VERIFICATION_NUMBER) {
                        val songDatabase = Room.databaseBuilder(
                                context, SongDatabase::class.java, MediaData.SONG_DATABASE_NAME).build()
                        songDatabase.songDAO().deleteAll()
                    }
                }
            }
        }
    }

    private fun attemptLoadFile(context: Context, mediaData: MediaData, fileSave: String): Long {
        var longEOF = 0L
        val file = File(context.filesDir, fileSave)
        if (file.exists()) {
            try {
                context.openFileInput(fileSave).use { fileInputStream ->
                    ObjectInputStream(fileInputStream).use { objectInputStream ->
                        settingsRepo.setSettings(objectInputStream.readObject() as Settings)
                        mediaData.setMasterPlaylist(objectInputStream.readObject() as RandomPlaylist)
                        val playlistSize = objectInputStream.readInt()
                        for (i in 0 until playlistSize) {
                            mediaData.addPlaylist(objectInputStream.readObject() as RandomPlaylist)
                        }
                        longEOF = objectInputStream.readLong()
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        return longEOF
    }

    fun saveFile(context: Context) {
        ServiceMain.executorServicePool.submit {
            val mediaData: MediaData = MediaData.getInstance(context)
            val file = File(context.filesDir, FILE_SAVE)
            val file2 = File(context.filesDir, FILE_SAVE2)
            val file3 = File(context.filesDir, FILE_SAVE3)
            if (file3.exists()) {
                file3.delete()
            }
            file2.renameTo(file3)
            file.renameTo(file2)
            val file4 = File(context.filesDir, FILE_SAVE)
            file4.delete()
            try {
                context.openFileOutput(FILE_SAVE, Context.MODE_PRIVATE).use { fos ->
                    ObjectOutputStream(fos).use { objectOutputStream ->
                        objectOutputStream.writeObject(settingsRepo.getSettings())
                        objectOutputStream.writeObject(mediaData.getMasterPlaylist())
                        objectOutputStream.writeInt(mediaData.getPlaylists().size)
                        for (randomPlaylist in mediaData.getPlaylists()) {
                            objectOutputStream.writeObject(randomPlaylist)
                        }
                        objectOutputStream.writeLong(SAVE_FILE_VERIFICATION_NUMBER)
                        objectOutputStream.flush()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
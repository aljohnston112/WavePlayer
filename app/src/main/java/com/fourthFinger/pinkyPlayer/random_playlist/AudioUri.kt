package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import java.io.*

class AudioUri(
    val displayName: String,
    val artist: String,
    val title: String,
    val id: Long
) : Comparable<AudioUri>, Serializable {

    private var duration: Long = -1
    fun getDuration(context: Context): Long {
        if (duration == -1L) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, getUri(id))
            var time =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (time == null) {
                time = "0"
            }
            duration = time.toLong()
            mediaMetadataRetriever.release()
        }
        return duration
    }

    override fun compareTo(other: AudioUri): Int {
        return title.compareTo(other.title)
    }

    override fun equals(other: Any?): Boolean {
        return other is AudioUri && getUri(id) == getUri(other.id)
    }

    override fun hashCode(): Int {
        return getUri(id).toString().hashCode()
    }

    fun getUri(): Uri {
        return getUri(id)
    }

    companion object {
        fun saveAudioUri(context: Context, audioUri: AudioUri) {
            try {
                context.openFileOutput(audioUri.id.toString(), Context.MODE_PRIVATE).use { fos ->
                    ObjectOutputStream(fos).use { objectOutputStream ->
                        objectOutputStream.writeObject(audioUri)
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun getAudioUri(context: Context, songID: Long): AudioUri? {
            val file = File(context.filesDir, songID.toString())
            if (file.exists()) {
                try {
                    context.openFileInput(songID.toString()).use { fileInputStream ->
                        ObjectInputStream(fileInputStream).use { objectInputStream ->
                            return objectInputStream.readObject() as AudioUri
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
            val projection = arrayOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.TITLE
            )
            val selection = MediaStore.Audio.Media._ID + " == ?"
            val selectionArgs = arrayOf(songID.toString())
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, null
            ).use { cursor ->
                if (cursor != null) {
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(nameCol)
                        val title = cursor.getString(titleCol)
                        val artist = cursor.getString(artistCol)
                        val audioUri = AudioUri(displayName, artist, title, songID)
                        saveAudioUri(context, audioUri)
                        return audioUri
                    }
                }
            }
            return null
        }

        fun getUri(songID: Long): Uri {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songID)
        }

    }

}
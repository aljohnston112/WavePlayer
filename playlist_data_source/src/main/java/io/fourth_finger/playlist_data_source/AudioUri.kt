package io.fourth_finger.playlist_data_source

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
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

    fun getUri(): Uri {
        return getUri(id)
    }

    fun getBitmap(context: Context): Bitmap? {
        // TODO 92? Seems to get resized for the Notification
        return BitmapUtil.getThumbnail(
            getUri(),
            92,
            92,
            context
        )
    }

    /**
     * Gets the duration of the song in milliseconds.
     *
     * @return The duration of the song in milliseconds or 0
     * if unable to retrieve the duration.
     */
    fun getDurationMS(context: Context): Long {
        if (duration == -1L) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(
                context,
                getUri(id)
            )
            var time =
                mediaMetadataRetriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )
            if (time == null) {
                time = "0"
            }
            duration = time.toLong()
            mediaMetadataRetriever.release()
        }
        return duration
    }

    override fun compareTo(other: AudioUri): Int {
        var i = title.compareTo(other.title)
        if (i == 0) {
            i = id.compareTo(other.id)
        }
        return i
    }

    // If two songs have the same id, then they have the same title
    override fun equals(other: Any?): Boolean {
        return other is AudioUri &&
                getUri(id) == getUri(other.id)
    }

    override fun hashCode(): Int {
        return getUri(id).toString().hashCode()
    }

    companion object {

        /**
         * Saves the given AudioUri to persistent storage.
         *
         * @param context
         * @param audioUri The AudioUri to save to persistent storage.
         */
        fun saveAudioUri(
            context: Context,
            audioUri: AudioUri
        ) {
            try {
                context.openFileOutput(
                    audioUri.id.toString(),
                    Context.MODE_PRIVATE
                ).use { fileOutputStream ->
                    ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                        objectOutputStream.writeObject(audioUri)
                    }
                }
                // TODO better error handling
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        /**
         * Gets the Uri corresponding to the given song id.
         *
         * @param songID The song id of the Uri to get.
         *
         * @return The Uri if it was found, else null.
         */
        fun getUri(songID: Long): Uri {
            return ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                songID
            )
        }

        /**
         * Gets the AudioUri corresponding to the given song id.
         *
         * @param context
         * @param songID The song id of the AudioUri to get.
         *
         * @return The AudioUri if it was found, else null.
         */
        fun getAudioUri(
            context: Context,
            songID: Long
        ): AudioUri? {
            var audioUri: AudioUri? = null
            val file = File(
                context.filesDir,
                songID.toString()
            )
            if (file.exists()) {
                audioUri = tryLoadingAudioUri(
                    context,
                    songID
                )
            }
            if (audioUri == null) {
                audioUri = tryCreatingAudioUri(
                    context,
                    songID
                )
            }
            return audioUri
        }

        /**
         * Tries to open the AudioUri file that corresponds to the given song id and
         * return its AudioUri.
         *
         * @param context
         * @param songID The song id of the AudioUri being requested.
         *
         * @return The AudioUri with the given song id
         * if it was on the file system, else null.
         */
        private fun tryLoadingAudioUri(
            context: Context,
            songID: Long
        ): AudioUri? {
            var audioUri: AudioUri? = null
            try {
                context.openFileInput(songID.toString()).use { fileInputStream ->
                    ObjectInputStream(fileInputStream).use { objectInputStream ->
                        audioUri = objectInputStream.readObject() as AudioUri
                    }
                }
                // TODO better error handling
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
            return audioUri
        }

        /**
         * Searches the MediaStore for a song with the given id.
         * If a tuple is found, then an AudioUri is created and returned.
         *
         * @param context
         * @param songID The id of the song to search the MediaStore for.
         *
         * @return An AudioUri of the song with the given song id
         * if a song with the given id exists in the MediaStore, else null.
         */
        private fun tryCreatingAudioUri(
            context: Context,
            songID: Long
        ): AudioUri? {
            var audioUri: AudioUri? = null
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.TITLE
            )
            val selection = MediaStore.Audio.Media._ID + " == ?"
            val selectionArgs = arrayOf(songID.toString())
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            ).use { cursor ->
                if (cursor != null) {
                    val nameCol = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.DISPLAY_NAME
                    )
                    val titleCol = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.TITLE
                    )
                    val artistCol = cursor.getColumnIndexOrThrow(
                        MediaStore.Audio.Media.ARTIST_ID
                    )
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(nameCol)
                        val title = cursor.getString(titleCol)
                        val artist = cursor.getString(artistCol)
                        val newAudioUri = AudioUri(
                            displayName,
                            artist,
                            title,
                            songID
                        )
                        saveAudioUri(
                            context,
                            newAudioUri
                        )
                        audioUri = newAudioUri
                    }
                }
            }
            return audioUri
        }

    }

}
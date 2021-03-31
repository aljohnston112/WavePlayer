package com.example.waveplayer.random_playlist

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import java.io.*
import java.util.*

class AudioUri(
        val displayName: String,
        val artist: String,
        val title: String,
        val id: Long
        ) : Comparable<AudioUri>, Serializable {

    val uri: Uri by lazy{
        getUri(id)
    }

    private val nestProbMap: NestedProbMap = NestedProbMap()

    private var duration: Int = -1
    fun getDuration(context: Context): Int {
        if (duration == -1) {
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, uri)
            var time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (time == null) {
                time = "00:00:00"
            }
            duration = time.toInt()
            mediaMetadataRetriever.release()
        }
        return duration
    }

    fun shouldPlay(random: Random): Boolean {
        return nestProbMap.outcome(random)
    }

    fun bad(percent: Double): Boolean {
        return nestProbMap.bad(percent)
    }

    fun good(percent: Double): Boolean {
        return nestProbMap.good(percent)
    }

    fun clearProbabilities() {
        nestProbMap.clearProbabilities()
    }

    override fun compareTo(other: AudioUri): Int {
        return title.compareTo(other.title)
    }

    override fun equals(other: Any?): Boolean {
        return other is AudioUri && uri == other.uri
    }

    override fun hashCode(): Int {
        return uri.toString().hashCode()
    }

    companion object {
        fun getAudioUri(context: Context, songID: Long): AudioUri? {
            val file = File(context.filesDir, songID.toString())
            if (file.exists()) {
                try {
                    context.openFileInput(songID.toString()).use { fileInputStream ->
                        ObjectInputStream(fileInputStream).use { objectInputStream ->
                            return objectInputStream.readObject() as AudioUri } }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
            return null
        }

        fun getUri(songID: Long): Uri {
            return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songID)
        }

    }
}
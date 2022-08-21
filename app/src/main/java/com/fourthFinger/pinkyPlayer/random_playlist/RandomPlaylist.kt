package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import java.io.Serializable
import java.util.*

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 * A playlist where a group of media files are picked from randomly.
 */

/**
 * Creates a random playlist.
 *
 * @param name            The name of this RandomPlaylist.
 * @param music           The List of AudioURIs to add to this playlist.
 * @throws IllegalArgumentException if there is not at least one AudioURI in music.
 * @throws IllegalArgumentException if folder is not a directory.
 */
class RandomPlaylist constructor(
    context: Context,
    name: String,
    music: MutableList<Song>,
    comparable: Boolean,
    maxPercent: Double
) : Serializable {

    private val playlistArray: MutableList<Long> = ArrayList()

    @Transient
    private var playlistIterator: MutableListIterator<Long>

    private var name: String
    fun getName(): String {
        return name
    }

    fun setName(context: Context, name: String) {
        this.name = name
        SaveFile.saveFile(context)
    }

    // The ProbFun that randomly picks the media to play
    private var probabilityFunction: ProbFun<Song>

    init {
        require(music.isNotEmpty()) { "List music must contain at least one AudioURI" }
        val files: MutableSet<Song> = LinkedHashSet(music)
        probabilityFunction = if (comparable) {
            ProbFun.ProbFunTreeMap(files, maxPercent)
        } else {
            ProbFun.ProbFunLinkedMap(files, maxPercent)
        }
        this.name = name
        for (song in music) {
            playlistArray.add(song.id)
        }
        playlistIterator = playlistArray.listIterator()
        SaveFile.saveFile(context)
    }

    fun getSongs(): Set<Song> {
        return probabilityFunction.getKeys()
    }

    fun getSongIDs(): List<Long> {
        return playlistArray
    }

    fun add(context: Context, song: Song) {
        probabilityFunction.add(song)
        playlistArray.add(song.id)
        SaveFile.saveFile(context)
    }

    fun add(context: Context, song: Song, probability: Double) {
        probabilityFunction.add(song, probability)
        playlistArray.add(song.id)
        SaveFile.saveFile(context)
    }

    fun remove(context: Context, song: Song) {
        probabilityFunction.remove(song)
        playlistArray.remove(song.id)
        SaveFile.saveFile(context)
    }

    @Deprecated(
        "Use contains(Long) instead",
        ReplaceWith(
            expression = "contains(song.id)",
            imports = emptyArray()
        ),
        DeprecationLevel.WARNING
    )
    operator fun contains(song: Song): Boolean {
        return probabilityFunction.contains(song)
    }

    operator fun contains(songID: Long): Boolean {
        return playlistArray.contains(songID)
    }

    fun getProbability(song: Song): Double {
        return probabilityFunction.getProbability(song)
    }

    fun resetProbabilities(context: Context) {
        probabilityFunction.resetProbabilities()
        SaveFile.saveFile(context)
    }

    fun lowerProbabilities(context: Context, lowerProb: Double) {
        probabilityFunction.lowerProbs(lowerProb)
        SaveFile.saveFile(context)
    }

    fun next(context: Context): AudioUri? {
        val song: Song = probabilityFunction.next()
        return AudioUri.getAudioUri(context, song.id)
    }

    fun size(): Int {
        return probabilityFunction.size()
    }

    fun swapSongPositions(context: Context, oldPosition: Int, newPosition: Int) {
        (probabilityFunction as? ProbFun.ProbFunLinkedMap)?.swapTwoPositions(oldPosition, newPosition)
        SaveFile.saveFile(context)
    }

    fun switchSongPositions(context: Context, oldPosition: Int, newPosition: Int) {
        (probabilityFunction as? ProbFun.ProbFunLinkedMap)?.switchOnesPosition(oldPosition, newPosition)
        SaveFile.saveFile(context)
    }

    fun next(context: Context, looping: Boolean, shuffling: Boolean): AudioUri? {
        return if (shuffling) {
            next(context)
        } else {
            if (looping && !playlistIterator.hasNext()) {
                playlistIterator = playlistArray.listIterator()
            }
            if (!playlistIterator.hasNext()) {
                null
            } else AudioUri.getAudioUri(context, playlistIterator.next())
        }
    }

    fun previous(
        context: Context,
        looping: Boolean,
        shuffling: Boolean
    ): AudioUri? {
        return if (shuffling) {
            // TODO play previous
            next(context)
        } else {
            if (looping && !playlistIterator.hasPrevious()) {
                playlistIterator = playlistArray.listIterator(playlistArray.size - 1)
            }
            AudioUri.getAudioUri(context, playlistIterator.previous())
        }
    }

    fun setIndexTo(songID: Long) {
        val i = playlistArray.indexOf(songID)
        playlistIterator = playlistArray.listIterator(i + 1)
    }

    fun bad(context: Context, song: Song, percentChangeDown: Double) {
        probabilityFunction.bad(song, percentChangeDown)
        SaveFile.saveFile(context)
    }

    fun good(context: Context, song: Song, percentChangeUp: Double) {
        probabilityFunction.good(song, percentChangeUp)
        SaveFile.saveFile(context)
    }

    override fun equals(other: Any?): Boolean {
        return other is RandomPlaylist && other.getName() == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        private const val serialVersionUID = 2323326608918863420L
    }

}
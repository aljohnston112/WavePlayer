package com.example.pinky_player.random_playlist

import android.content.*
import java.io.Serializable
import java.util.*

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 * A playlist where a group of media files are picked from randomly.
 */
class RandomPlaylist constructor(name: String, music: MutableList<Song>, maxPercent: Double,
                     comparable: Boolean) : Serializable {
    private val playlistArray: MutableList<Long> = ArrayList()

    @Transient
    private var playlistIterator: MutableListIterator<Long>
    private var name: String
    fun getName(): String {
        return name
    }

    fun setName(name: String) {
        this.name = name
    }

    // The ProbFun that randomly picks the media to play
    private val probabilityFunction: ProbFun<Song>
    fun getSongs(): List<Song> {
        return probabilityFunction.getKeys()
    }

    fun getSongIDs(): List<Long> {
        return playlistArray
    }

    fun add(song: Song) {
        probabilityFunction.add(song)
        playlistArray.add(song.id)
    }

    fun add(song: Song, probability: Double) {
        probabilityFunction.add(song, probability)
        playlistArray.add(song.id)
    }

    fun remove(song: Song) {
        probabilityFunction.remove(song)
        playlistArray.remove(song.id)
    }

    operator fun contains(song: Song): Boolean {
        return probabilityFunction.contains(song)
    }

    operator fun contains(songID: Long): Boolean {
        return playlistArray.contains(songID)
    }

    fun setMaxPercent(maxPercent: Double) {
        probabilityFunction.setMaxPercent(maxPercent)
    }

    fun good(context: Context, song: Song, percent: Double, scale: Boolean) {
        if (AudioUri.getAudioUri(context, song.id)?.good(percent) == true) {
            probabilityFunction.good(song, percent, scale)
        }
    }

    fun bad(context: Context, song: Song, percent: Double) {
        if (AudioUri.getAudioUri(context, song.id)?.bad(percent) == true) {
            probabilityFunction.bad(song, percent)
        }
    }

    fun getProbability(song: Song): Double {
        return probabilityFunction.getProbability(song)
    }

    fun clearProbabilities(context: Context) {
        for (song in probabilityFunction.getKeys()) {
            AudioUri.getAudioUri(context, song.id)?.clearProbabilities()
        }
        probabilityFunction.clearProbabilities()
    }

    fun lowerProbabilities(context: Context, lowerProb: Double) {
        probabilityFunction.lowerProbs(lowerProb)
    }

    fun next(context: Context, random: Random): AudioUri? {
        var song: Song
        var audioUri: AudioUri? = null
        var next = false
        while (!next) {
            song = probabilityFunction.`fun`(random)
            audioUri = AudioUri.getAudioUri(context, song.id)!!
            next = audioUri.shouldPlay(random)
        }
        return audioUri
    }

    fun size(): Int {
        return probabilityFunction.size()
    }

    fun swapSongPositions(oldPosition: Int, newPosition: Int) {
        probabilityFunction.swapPositions(oldPosition, newPosition)
    }

    fun switchSongPositions(oldPosition: Int, newPosition: Int) {
        probabilityFunction.switchPositions(oldPosition, newPosition)
    }

    fun next(context: Context, random: Random, looping: Boolean, shuffling: Boolean): AudioUri? {
        return if (shuffling) {
            next(context, random)
        } else {
            if (looping && !playlistIterator.hasNext()) {
                playlistIterator = playlistArray.listIterator()
            }
            if (!playlistIterator.hasNext()) {
                null
            } else AudioUri.getAudioUri(context, playlistIterator.next())
        }
    }

    fun previous(context: Context, random: Random, looping: Boolean, shuffling: Boolean): AudioUri? {
        return if (shuffling) {
            next(context, random)
        } else {
            if (looping && !playlistIterator.hasPrevious()) {
                playlistIterator = playlistArray.listIterator(playlistArray.size - 1)
            }
            AudioUri.getAudioUri(context, playlistIterator.previous())
        }
    }

    /*
    public void goToFront() {
        playlistIterator = null;
        playlistIterator = playlistArray.listIterator();
    }

    public void goToBack() {
        playlistIterator = null;
        playlistIterator = playlistArray.listIterator(playlistArray.size());
    }

     */
    fun setIndexTo(songID: Long) {
        if (playlistArray != null) {
            val i = playlistArray.indexOf(songID)
            playlistIterator = playlistArray.listIterator(i + 1)
        }
    }

    fun globalBad(song: Song, percentChangeDown: Double) {
        probabilityFunction.bad(song, percentChangeDown)
    }

    fun globalGood(song: Song, percentChangeUp: Double) {
        probabilityFunction.good(song, percentChangeUp, true)
    }

    companion object {
        private const val serialVersionUID = 2323326608918863420L
    }

    /**
     * Creates a random playlist.
     *
     * @param name            The name of this RandomPlaylist.
     * @param music           The List of AudioURIs to add to this playlist.
     * @param maxPercent      The max percentage that any AudioUri can have
     * of being returned when fun() is called.
     * @throws IllegalArgumentException if there is not at least one AudioURI in music.
     * @throws IllegalArgumentException if folder is not a directory.
     */
    init {
        require(music.isNotEmpty()) { "List music must contain at least one AudioURI" }
        val files: MutableSet<Song> = LinkedHashSet(music)
        probabilityFunction = if (comparable) {
            ProbFunTreeMap(files, maxPercent)
        } else {
            ProbFunLinkedMap(files, maxPercent)
        }
        this.name = name
        for (song in music) {
            playlistArray.add(song.id)
        }
        playlistIterator = playlistArray.listIterator()
    }
}
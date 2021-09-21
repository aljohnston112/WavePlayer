package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.*
import androidx.recyclerview.widget.DiffUtil
import java.io.Serializable
import java.util.*

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 * A playlist where a group of media files are picked from randomly.
 */
class RandomPlaylist constructor(
    name: String,
    music: MutableList<Song>,
    maxPercent: Double,
    comparable: Boolean
) : Serializable {

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
    private var probabilityFunction: ProbFun<Song>
    fun getSongs(): Set<Song> {
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

    fun clearProbabilities() {
        probabilityFunction.resetProbabilities()
    }

    fun lowerProbabilities(lowerProb: Double) {
        probabilityFunction.lowerProbs(lowerProb)
    }

    fun next(context: Context, random: Random): AudioUri? {
        val song: Song = probabilityFunction.next(random)
        return AudioUri.getAudioUri(context, song.id)
    }

    fun size(): Int {
        return probabilityFunction.size()
    }

    fun swapSongPositions(oldPosition: Int, newPosition: Int) {
        (probabilityFunction as? ProbFun.ProbFunLinkedMap)?.swapTwoPositions(oldPosition, newPosition)
    }

    fun switchSongPositions(oldPosition: Int, newPosition: Int) {
        (probabilityFunction as? ProbFun.ProbFunLinkedMap)?.switchOnesPosition(oldPosition, newPosition)
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

    fun previous(
        context: Context,
        random: Random,
        looping: Boolean,
        shuffling: Boolean
    ): AudioUri? {
        return if (shuffling) {
            // TODO play previous
            next(context, random)
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

    fun bad(song: Song) {
        probabilityFunction.bad(song)
    }

    fun good(song: Song) {
        probabilityFunction.good(song)
    }

    companion object {
        private const val serialVersionUID = 2323326608918863420L
    }

    /**
     * Creates a random playlist.
     *
     * @param name            The name of this RandomPlaylist.
     * @param music           The List of AudioURIs to add to this playlist.
     * @throws IllegalArgumentException if there is not at least one AudioURI in music.
     * @throws IllegalArgumentException if folder is not a directory.
     */
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
    }

    class DiffUtilItemCallbackPlaylists : DiffUtil.ItemCallback<RandomPlaylist>() {
        override fun areItemsTheSame(oldItem: RandomPlaylist, newItem: RandomPlaylist): Boolean {
            return oldItem.getName() == newItem.getName()
        }

        override fun areContentsTheSame(oldItem: RandomPlaylist, newItem: RandomPlaylist): Boolean {
            return true
        }

    }

}
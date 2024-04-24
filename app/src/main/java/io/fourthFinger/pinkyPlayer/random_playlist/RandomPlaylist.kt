package io.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import java.io.Serializable
import java.util.*

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 *
 * A playlist where a group of media files are picked
 * from a adjustable probability distribution.
 *
 * @param name The name of this RandomPlaylist.
 * @param music The List of AudioURIs to add to this playlist.
 * @param isSorted Whether or not the playlist is alphabetically sorted.
 * @param maxPercent The max percent that a song can have.
 *
 */
class RandomPlaylist(
    name: String,
    music: List<Song>,
    isSorted: Boolean,
    maxPercent: Double,
) : Serializable {

    private var probabilityFunction: ProbabilityFunction<Song>
    private val playlistArray: MutableList<Long> = ArrayList()

    private var name: String

    init {
        val files: MutableSet<Song> = LinkedHashSet(music)
        probabilityFunction = if (isSorted) {
            ProbabilityFunction.ProbabilityFunctionTreeMap(
                files,
                maxPercent
            )
        } else {
            ProbabilityFunction.ProbabilityFunctionLinkedMap(
                files,
                maxPercent
            )
        }
        this.name = name
        for (song in music) {
            playlistArray.add(song.id)
        }
    }

    fun getName(): String {
        return name
    }

    fun setName(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        name: String
    ) {
        this.name = name
        SaveFile.saveFile(
            context,
            playlistsRepo
        )
    }

    fun getSongs(): Set<Song> {
        return probabilityFunction.getItems()
    }

    fun getSongIDs(): List<Long> {
        return playlistArray
    }

    fun add(song: Song) {
        probabilityFunction.add(song)
        playlistArray.add(song.id)
    }

    fun add(
        song: Song,
        probability: Double
    ) {
        probabilityFunction.add(
            song,
            probability
        )
        playlistArray.add(song.id)
    }

    fun remove(song: Song) {
        probabilityFunction.remove(song)
        playlistArray.remove(song.id)
    }

    fun getProbability(song: Song): Double {
        return probabilityFunction.getProbability(song)
    }

    fun resetProbabilities(
        context: Context,
        playlistsRepo: PlaylistsRepo
    ) {
        probabilityFunction.resetProbabilities()
        SaveFile.saveFile(
            context,
            playlistsRepo
        )
    }

    fun lowerProbabilities(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        probabilityFloor: Double
    ) {
        probabilityFunction.lowerProbabilities(probabilityFloor)
        SaveFile.saveFile(
            context,
            playlistsRepo
        )
    }

    fun nextRandomSong(context: Context): AudioUri? {
        val song: Song = probabilityFunction.next()
        return AudioUri.getAudioUri(
            context,
            song.id
        )
    }

    fun size(): Int {
        return probabilityFunction.getSize()
    }

    fun swapSongPositions(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        oldPosition: Int,
        newPosition: Int
    ) {
        (probabilityFunction as? ProbabilityFunction.ProbabilityFunctionLinkedMap)?.swapTwoItems(
            oldPosition,
            newPosition
        )
        SaveFile.saveFile(
            context,
            playlistsRepo
        )
    }

    fun switchSongPositions(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        oldPosition: Int,
        newPosition: Int
    ) {
        (probabilityFunction as? ProbabilityFunction.ProbabilityFunctionLinkedMap)?.switchOnesPosition(
            oldPosition,
            newPosition
        )
        SaveFile.saveFile(
            context,
            playlistsRepo
        )
    }

    fun bad(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        song: Song,
        percentChangeDown: Double
    ) {
        probabilityFunction.bad(
            song,
            percentChangeDown
        )
        SaveFile.saveFile(
            context,
            playlistsRepo
        )
    }

    fun good(
        context: Context,
        playlistsRepo: PlaylistsRepo,
        song: Song,
        percentChangeUp: Double
    ) {
        probabilityFunction.good(
            song,
            percentChangeUp
        )
        SaveFile.saveFile(
            context,
            playlistsRepo
        )
    }

    operator fun contains(songID: Long): Boolean {
        return playlistArray.contains(songID)
    }

    override fun equals(other: Any?): Boolean {
        return other is RandomPlaylist && other.getName() == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}
package io.fourthFinger.playlistDataSource

import android.content.Context
import java.io.Serializable
import java.util.*

/**
 * @author Alexander Johnston
 * @since Copyright 2019
 *
 * A playlist where a group of media files are picked
 * from a adjustable probability distribution.
 * @param name The name of this RandomPlaylist.
 * @param music The List of AudioURIs to add to this playlist.
 * @param isSorted Whether or not the playlist is alphabetically sorted.
 * @param maxPercent The max percent that a song can have.
 */
class RandomPlaylist(
    name: String,
    music: List<Song>,
    isSorted: Boolean,
    maxPercent: Double
) : Serializable {

    var name = name
        set(value) { field = value }

    private var probabilityFunction: ProbabilityFunction<Song>

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
    }

    fun getSongs(): Set<Song> {
        return probabilityFunction.getItems()
    }

    fun getSongIDs(): List<Long> {
        return probabilityFunction.getItems().map { it.id }
    }

    fun getProbability(song: Song): Double {
        return probabilityFunction.getProbability(song)
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

    operator fun contains(songID: Long): Boolean {
        return probabilityFunction.getItems().map { it.id }.contains(songID)
    }

    override fun equals(other: Any?): Boolean {
        return other is RandomPlaylist && other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    internal fun setMaxPercent(maxPercent: Double) {
        probabilityFunction.maxPercent(maxPercent)
    }

    internal fun resetProbabilities() {
        probabilityFunction.resetProbabilities()
    }

    internal fun lowerProbabilities(
        probabilityFloor: Double
    ) {
        probabilityFunction.lowerProbabilities(probabilityFloor)
    }

    internal fun swapSongPositions(
        oldPosition: Int,
        newPosition: Int
    ) {
        (probabilityFunction as? ProbabilityFunction.ProbabilityFunctionLinkedMap)
            ?.swapTwoItems(
                oldPosition,
                newPosition
            )
    }

    internal fun switchSongPositions(
        oldPosition: Int,
        newPosition: Int
    ) {
        (probabilityFunction as? ProbabilityFunction.ProbabilityFunctionLinkedMap)
            ?.switchOnesPosition(
                oldPosition,
                newPosition
            )
    }

    internal fun bad(
        song: Song,
        percentChangeDown: Double
    ) {
        probabilityFunction.bad(
            song,
            percentChangeDown
        )
    }

    internal fun good(
        song: Song,
        percentChangeUp: Double
    ) {
        probabilityFunction.good(
            song,
            percentChangeUp
        )
    }

    internal fun add(
        song: Song
    ) {
        probabilityFunction.add(song)
    }

    internal fun add(
        song: Song,
        probability: Double
    ) {
        probabilityFunction.add(
            song,
            probability
        )
    }

    internal fun remove(song: Song) {
        probabilityFunction.remove(song)
    }

}
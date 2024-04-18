package io.fourthFinger.pinkyPlayer.random_playlist

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
class RandomPlaylist(
    context: Context,
    name: String,
    music: List<Song>,
    comparable: Boolean,
    maxPercent: Double,
    playlistsRepo: PlaylistsRepo
) : Serializable {

    private val playlistArray: MutableList<Long> = ArrayList()

    private var name: String
    fun getName(): String {
        return name
    }

    fun setName(context: Context, playlistsRepo: PlaylistsRepo, name: String) {
        this.name = name
        SaveFile.saveFile(context, playlistsRepo)
    }

    // The ProbFun that randomly picks the media to play
    private var probabilityFunction: ProbFun<Song>

    init {
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
        SaveFile.saveFile(context, playlistsRepo)
    }

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

    fun resetProbabilities(context: Context, playlistsRepo: PlaylistsRepo) {
        probabilityFunction.resetProbabilities()
        SaveFile.saveFile(context, playlistsRepo)
    }

    fun lowerProbabilities(context: Context, playlistsRepo: PlaylistsRepo, lowerProb: Double) {
        probabilityFunction.lowerProbabilities(lowerProb)
        SaveFile.saveFile(context, playlistsRepo)
    }

    fun nextRandomSong(context: Context): AudioUri? {
        val song: Song = probabilityFunction.next()
        return AudioUri.getAudioUri(context, song.id)
    }

    fun size(): Int {
        return probabilityFunction.size()
    }

    fun swapSongPositions(context: Context, playlistsRepo: PlaylistsRepo, oldPosition: Int, newPosition: Int) {
        (probabilityFunction as? ProbFun.ProbFunLinkedMap)?.swapTwoPositions(
            oldPosition,
            newPosition
        )
        SaveFile.saveFile(context, playlistsRepo)
    }

    fun switchSongPositions(context: Context, playlistsRepo: PlaylistsRepo, oldPosition: Int, newPosition: Int) {
        (probabilityFunction as? ProbFun.ProbFunLinkedMap)?.switchOnesPosition(
            oldPosition,
            newPosition
        )
        SaveFile.saveFile(context, playlistsRepo)
    }

    fun bad(context: Context, playlistsRepo: PlaylistsRepo, song: Song, percentChangeDown: Double) {
        val i = probabilityFunction.bad(song, percentChangeDown)
        SaveFile.saveFile(context, playlistsRepo)
    }

    fun good(context: Context, playlistsRepo: PlaylistsRepo, song: Song, percentChangeUp: Double) {
        probabilityFunction.good(song, percentChangeUp)
        SaveFile.saveFile(context, playlistsRepo)
    }

    override fun equals(other: Any?): Boolean {
        return other is RandomPlaylist && other.getName() == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    companion object {
        private const val SERIAL_VERSION_UID = 2323326608918863420L
    }

}
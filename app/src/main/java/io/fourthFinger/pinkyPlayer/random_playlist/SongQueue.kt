package io.fourthFinger.pinkyPlayer.random_playlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.fourthFinger.playlistDataSource.Song
import java.util.LinkedList

class SongQueue(
    private val songRepo: SongRepo
) {

    private val _songQueue: LinkedList<Song> = LinkedList()
    private val mLDSongQueue: MutableLiveData<LinkedList<Song>> = MutableLiveData(LinkedList())
    val songQueue: LiveData<Set<Song>> = mLDSongQueue.map {
        it.toSet()
    }
    private var songQueueIterator: MutableListIterator<Song>

    operator fun hasNext(): Boolean {
        return songQueueIterator.hasNext()
    }

    operator fun next(): Song {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return songQueueIterator.next()
    }

    fun hasPrevious(): Boolean {
        return songQueueIterator.hasPrevious()
    }

    fun previous(): Song {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }
        return songQueueIterator.previous()
    }

    fun clearSongQueue() {
        _songQueue.clear()
        mLDSongQueue.value = _songQueue
        songQueueIterator = _songQueue.listIterator()
    }

    fun goToFront() {
        songQueueIterator = _songQueue.listIterator()
    }

    fun goToBack() {
        songQueueIterator = _songQueue.listIterator(_songQueue.size)
    }

    fun addToQueue(songID: Long) {
        val i = songQueueIterator.nextIndex()
        goToBack()
        songRepo.getSong(songID)?.let { songQueueIterator.add(it) }
        songQueueIterator = _songQueue.listIterator(i)
        mLDSongQueue.value = _songQueue
    }

    fun clearQueueAndAddSong(songID: Long) {
        clearSongQueue()
        addToQueue(songID)
    }

    // TODO update FragmentQueue after all moves have been completed!

    fun notifySongMoved(from: Int, to: Int) {
        var i = songQueueIterator.nextIndex()
        if (i == from) {
            i++
        } else if (i - 1 == to) {
            i--
        }
        songQueueIterator = _songQueue.listIterator(from)
        val song = songQueueIterator.next()
        songQueueIterator.remove()
        songQueueIterator = _songQueue.listIterator(to)
        songQueueIterator.add(song)
        songQueueIterator = _songQueue.listIterator(i)
    }

    private var undoSong: Song? = null

    fun notifySongRemoved(position: Int): Boolean {
        var i = songQueueIterator.nextIndex()
        var playing = false
        if (position == i - 1) {
            playing = true
        }
        if (i == _songQueue.size) {
            i--
        }
        songQueueIterator = _songQueue.listIterator(position)
        undoSong = songQueueIterator.next()
        songQueueIterator.remove()
        if (playing && i != _songQueue.size) {
            i--
        }
        songQueueIterator = _songQueue.listIterator(i)
        mLDSongQueue.value = _songQueue
        return playing
    }

    fun notifyItemInserted(position: Int) {
        val i = songQueueIterator.nextIndex()
        songQueueIterator = _songQueue.listIterator(position)
        undoSong?.let { songQueueIterator.add(it) }
        undoSong = null
        songQueueIterator = _songQueue.listIterator(i)
        mLDSongQueue.value = _songQueue
    }

    fun setIndex(position: Int): Song {
        songQueueIterator = _songQueue.listIterator(position)
        val song = songQueueIterator.next()
        songQueueIterator.previous()
        return song
    }

    init {
        songQueueIterator = _songQueue.listIterator()
    }

}
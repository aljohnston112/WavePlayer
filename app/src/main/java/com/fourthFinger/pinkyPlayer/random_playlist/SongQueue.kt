package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import java.util.*

class SongQueue private constructor() {

    private val songQueue: LinkedList<Song> = LinkedList()
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

    private fun clearSongQueue() {
        songQueue.clear()
        songQueueIterator = songQueue.listIterator()
    }

    fun goToFront() {
        songQueueIterator = songQueue.listIterator()
    }

    fun goToBack() {
        songQueueIterator = songQueue.listIterator(songQueue.size)
    }

    fun addToQueue(context: Context, songID: Long) {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        val i = songQueueIterator.nextIndex()
        goToBack()
        playlistsRepo.getSong(songID)?.let { songQueueIterator.add(it) }
        songQueueIterator = songQueue.listIterator(i)
    }

    fun newSessionStarted(context: Context, song: Long) {
        clearSongQueue()
        addToQueue(context, song)
    }

    fun queue(): Set<Song> {
        return songQueue.toSet()
    }

    fun notifySongMoved(from: Int, to: Int) {
        val i = songQueueIterator.nextIndex()
        songQueueIterator = songQueue.listIterator(from)
        val song = songQueueIterator.next()
        songQueueIterator.remove()
        songQueueIterator = songQueue.listIterator(to)
        songQueueIterator.add(song)
        songQueueIterator = songQueue.listIterator(i)
    }

    private var undoSong: Song? = null

    fun notifySongRemoved(position: Int) {
        var i = songQueueIterator.nextIndex()
        if(i == songQueue.size){
            i--
        }
        songQueueIterator = songQueue.listIterator(position)
        undoSong = songQueueIterator.next()
        songQueueIterator.remove()
        songQueueIterator = songQueue.listIterator(i)
    }

    fun notifyItemInserted(position: Int) {
        val i = songQueueIterator.nextIndex()
        songQueueIterator = songQueue.listIterator(position)
        undoSong?.let { songQueueIterator.add(it) }
        undoSong = null
        songQueueIterator = songQueue.listIterator(i)
    }

    fun songClicked(pos: Int): Song {
        songQueueIterator = songQueue.listIterator(pos)
        val song =  songQueueIterator.next()
        songQueueIterator.previous()
        return song
    }

    init {
        songQueueIterator = songQueue.listIterator()
    }

    companion object {
        private var INSTANCE: SongQueue? = null

        @Synchronized
        fun getInstance(): SongQueue {
            if (INSTANCE == null) {
                INSTANCE = SongQueue()
            }
            return INSTANCE!!
        }

    }

}
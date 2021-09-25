package com.fourthFinger.pinkyPlayer.random_playlist

import java.util.*

class SongQueue private constructor() {

    // TODO Add methods to add and delete songs from the queue
    private val songQueue: LinkedList<Long> = LinkedList()
    private var songQueueIterator: MutableListIterator<Long>

    operator fun hasNext(): Boolean {
        return songQueueIterator.hasNext()
    }

    operator fun next(): Long {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return songQueueIterator.next()
    }
    fun hasPrevious(): Boolean {
        return songQueueIterator.hasPrevious()
    }
    fun previous(): Long {
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

    fun addToQueue(songID: Long) {
        val i = songQueueIterator.nextIndex()
        goToBack()
        songQueueIterator.add(songID)
        songQueueIterator = songQueue.listIterator(i)
        // TODO seems to be unneeded due to method name, but may have a purpose
        // songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(songID))
    }

    fun newSessionStarted(song: Song) {
        clearSongQueue()
        addToQueue(song.id)
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
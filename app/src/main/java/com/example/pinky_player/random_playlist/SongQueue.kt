package com.example.pinky_player.random_playlist

import java.util.*

class SongQueue private constructor() {

    // TODO Add methods to add and delete songs from the queue
    private val songQueue: LinkedList<Long> = LinkedList()
    private var songQueueIterator: MutableListIterator<Long>
    fun isEmpty(): Boolean {
        return songQueue.size == 0
    }
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
    fun clearSongQueue() {
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
        songQueue.add(songID)
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(songID))
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
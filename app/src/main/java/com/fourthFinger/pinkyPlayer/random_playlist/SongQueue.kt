package com.fourthFinger.pinkyPlayer.random_playlist

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import java.util.*

class SongQueue private constructor() {

    private val _songQueue: LinkedList<Song> = LinkedList()
    private val mLDSongQueue: MutableLiveData<LinkedList<Song>> = MutableLiveData(LinkedList())
    val songQueue: LiveData<Set<Song>> = Transformations.map(mLDSongQueue){
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

    private fun clearSongQueue() {
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

    fun addToQueue(context: Context, songID: Long) {
        val playlistsRepo = PlaylistsRepo.getInstance(context)
        val i = songQueueIterator.nextIndex()
        goToBack()
        playlistsRepo.getSong(songID)?.let { songQueueIterator.add(it) }
        songQueueIterator = _songQueue.listIterator(i)
        mLDSongQueue.value = _songQueue
    }

    fun newSessionStarted(context: Context, song: Long) {
        clearSongQueue()
        addToQueue(context, song)
    }

    fun queue(): Set<Song> {
        return _songQueue.toSet()
    }

    fun notifySongMoved(from: Int, to: Int) {
        var i = songQueueIterator.nextIndex()
        if(i-1 == from){
            i = to+1
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
        if(position == i-1){
            playing = true
        }
        if(i == _songQueue.size){
            i--
        }
        songQueueIterator = _songQueue.listIterator(position)
        undoSong = songQueueIterator.next()
        songQueueIterator.remove()
        if(playing && i != _songQueue.size){
            i--
        }
        songQueueIterator = _songQueue.listIterator(i)
        return playing
    }

    fun notifyItemInserted(position: Int) {
        val i = songQueueIterator.nextIndex()
        songQueueIterator = _songQueue.listIterator(position)
        undoSong?.let { songQueueIterator.add(it) }
        undoSong = null
        songQueueIterator = _songQueue.listIterator(i)
    }

    fun songClicked(pos: Int): Song {
        songQueueIterator = _songQueue.listIterator(pos)
        val song =  songQueueIterator.next()
        songQueueIterator.previous()
        return song
    }

    init {
        songQueueIterator = _songQueue.listIterator()
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
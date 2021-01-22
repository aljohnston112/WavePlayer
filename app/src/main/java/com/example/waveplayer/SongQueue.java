package com.example.waveplayer;

import android.util.Log;

import com.example.waveplayer.random_playlist.AudioUri;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class SongQueue {

    // TODO Add methods to add and delete songs from the queue, and a next song variable
    //  to allow for preloading of next song

    private final LinkedList<Long> songQueue = new LinkedList<>();
    private ListIterator<Long> songQueueIterator;

    public SongQueue(){
        songQueueIterator = songQueue.listIterator();
    }

    public boolean isEmpty() {
        return songQueue.size() == 0;
    }

    public boolean hasNext(){
        return songQueueIterator.hasNext();
    }
    
    public Long next(){
        if(!hasNext()){
            throw new NoSuchElementException();
        }
        return songQueueIterator.next();
    }

    public boolean hasPrevious(){
        return songQueueIterator.hasPrevious();
    }

    public Long previous(){
        if(!hasPrevious()){
            throw new NoSuchElementException();
        }
        return songQueueIterator.previous();
    }

    public void clearSongQueue() {
        songQueueIterator = null;
        songQueue.clear();
        songQueueIterator = songQueue.listIterator();
    }

    public void goToFront(){
        songQueueIterator = null;
        songQueueIterator = songQueue.listIterator();
    }

    public void goToBack(){
        songQueueIterator = null;
        songQueueIterator = songQueue.listIterator(songQueue.size());
    }

    public void addToQueue(Long songID){
        songQueueIterator = null;
        songQueue.add(songID);
        songQueueIterator = songQueue.listIterator(songQueue.lastIndexOf(songID));
        songQueueIterator.next();
    }

    public void addToQueueAtCurrentPosition(Long songID) {
        int pos = songQueueIterator.nextIndex();
        songQueueIterator = null;
        songQueue.add(songID);
        songQueueIterator = songQueue.listIterator(pos);
    }

}

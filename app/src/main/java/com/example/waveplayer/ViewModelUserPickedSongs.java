package com.example.waveplayer;


import androidx.annotation.GuardedBy;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ViewModelUserPickedSongs extends ViewModel {

    @GuardedBy("this")
    private final List<Song> userPickedSongs = new ArrayList<>();

    synchronized public List<Song> getUserPickedSongs() {
        return userPickedSongs;
    }

    synchronized public void addUserPickedSong(Song songs) {
        userPickedSongs.add(songs);
    }

    synchronized public void removeUserPickedSong(Song song) {
        userPickedSongs.remove(song);
    }

    synchronized public void clearUserPickedSongs() {
        userPickedSongs.clear();
    }

}
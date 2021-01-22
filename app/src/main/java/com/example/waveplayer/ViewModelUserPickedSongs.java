package com.example.waveplayer;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class ViewModelUserPickedSongs extends ViewModel {

    private final List<Song> userPickedSongs = new ArrayList<>();

    private final MutableLiveData<List<Song>> userPickedSongsMLD = new MutableLiveData<List<Song>>();

    public LiveData<List<Song>> getUserPickedSongs() {
        return userPickedSongsMLD;
    }

    public void addUserPickedSong(Song songs) {
        userPickedSongs.add(songs);
        userPickedSongsMLD.setValue(userPickedSongs);
    }

    public void removeUserPickedSong(Song song) {
        userPickedSongs.remove(song);
        userPickedSongsMLD.setValue(userPickedSongs);
    }

    public void clearUserPickedSongs() {
        userPickedSongs.clear();
        userPickedSongsMLD.setValue(userPickedSongs);
    }

}
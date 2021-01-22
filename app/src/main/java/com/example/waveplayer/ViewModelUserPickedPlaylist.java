package com.example.waveplayer;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.waveplayer.random_playlist.RandomPlaylist;

public class ViewModelUserPickedPlaylist extends ViewModel {

    private final MutableLiveData<RandomPlaylist> userPickedPlaylist = new MutableLiveData<RandomPlaylist>();;

    public LiveData<RandomPlaylist> getUserPickedPlaylist(){
        return userPickedPlaylist;
    }

    public void setUserPickedPlaylist(RandomPlaylist userPickedPlaylist) {
        this.userPickedPlaylist.setValue(userPickedPlaylist);
    }

}

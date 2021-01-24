package com.example.waveplayer;

import androidx.annotation.GuardedBy;
import androidx.lifecycle.ViewModel;

import com.example.waveplayer.random_playlist.RandomPlaylist;

public class ViewModelUserPickedPlaylist extends ViewModel {

    @GuardedBy("this")
    volatile private RandomPlaylist userPickedPlaylist;

    synchronized public RandomPlaylist getUserPickedPlaylist(){
        return userPickedPlaylist;
    }

    synchronized public void setUserPickedPlaylist(RandomPlaylist userPickedPlaylist) {
        this.userPickedPlaylist = userPickedPlaylist;
    }

}

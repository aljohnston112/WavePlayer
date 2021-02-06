package com.example.waveplayer.activity_main;

import android.view.View;

import androidx.annotation.GuardedBy;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.random_playlist.RandomPlaylist;
import com.example.waveplayer.random_playlist.Song;

public class ViewModelActivityMain extends ViewModel {

    private final MutableLiveData<Boolean> showFAB = new MutableLiveData<>();

    public LiveData<Boolean> showFab() {
        return showFAB;
    }

    public void showFab(boolean showFAB){
        this.showFAB.postValue(showFAB);
    }

    private MutableLiveData<String> actionBarTitle = new MutableLiveData<>();

    public LiveData<String> getActionBarTitle() {
        return actionBarTitle;
    }

    public void setActionBarTitle(String actionBarTitle){
        this.actionBarTitle.postValue(actionBarTitle);
    }

    private MutableLiveData<Integer> fabText = new MutableLiveData<>();

    public LiveData<Integer> getFABText() {
        return fabText;
    }

    public void setFABText(Integer fabText){
        this.fabText.postValue(fabText);
    }

    private MutableLiveData<Integer> fabImage = new MutableLiveData<>();

    public LiveData<Integer> getFABImage() {
        return fabImage;
    }

    public void setFabImage(Integer fabImage){
        this.fabImage.postValue(fabImage);
    }

    private MutableLiveData<View.OnClickListener> fabOnClickListener = new MutableLiveData<>();

    public LiveData<View.OnClickListener> getFabOnClickListener() {
        return fabOnClickListener;
    }

    public void setFabOnClickListener(View.OnClickListener fabOnClickListener){
        this.fabOnClickListener.postValue(fabOnClickListener);
    }

    private MutableLiveData<AudioUri> currentSong = new MutableLiveData<>();

    public LiveData<AudioUri> getCurrentSong() {
        return currentSong;
    }

    public void setCurrentSong(AudioUri audioUri){
        this.currentSong.postValue(audioUri);
    }

    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();

    public LiveData<Boolean> isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying){
        this.isPlaying.postValue(isPlaying);
    }

    @GuardedBy("this")
    volatile private RandomPlaylist playlistToAddToQueue;

    synchronized public RandomPlaylist getPlaylistToAddToQueue(){
        return playlistToAddToQueue;
    }

    synchronized public void setPlaylistToAddToQueue(RandomPlaylist playlistToAddToQueue) {
        this.playlistToAddToQueue = playlistToAddToQueue;
        this.songToAddToQueue = null;
    }

    @GuardedBy("this")
    volatile private Long songToAddToQueue;

    synchronized public Long getSongToAddToQueue(){
        return songToAddToQueue;
    }

    synchronized public void setSongToAddToQueue(Long songToAddToQueue) {
        this.songToAddToQueue = songToAddToQueue;
        this.playlistToAddToQueue = null;
    }

}
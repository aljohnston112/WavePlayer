package com.example.waveplayer.fragments.fragment_loading;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.waveplayer.media_controller.MediaController;
import com.example.waveplayer.media_controller.MediaData;

public class ViewModelFragmentLoading extends ViewModel {

    private final MediaData mediaData = MediaData.getInstance();

    public LiveData<Double> getLoadingProgress() {
        return mediaData.getLoadingProgress();
    }

    public LiveData<String> getLoadingText() {
        return mediaData.getLoadingText();
    }

}

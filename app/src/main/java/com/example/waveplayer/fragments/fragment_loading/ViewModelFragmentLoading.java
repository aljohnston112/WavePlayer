package com.example.waveplayer.fragments.fragment_loading;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.waveplayer.media_controller.MediaData;

public class ViewModelFragmentLoading extends ViewModel {

    private final MutableLiveData<Double> mLoadingProgress = new MutableLiveData<>(0.0);

    public LiveData<Double> getLoadingProgress() {
        return mLoadingProgress;
    }

    public void setLoadingProgress(double loadingProgress) {
        this.mLoadingProgress.setValue(loadingProgress);
    }

    private final Observer<Double> mObserverLoadingProgress = new Observer<Double>() {
        @Override
        public void onChanged(final Double loadingProgress) {
            setLoadingProgress(loadingProgress);
        }
    };

    private MutableLiveData<String> mLoadingText = new MutableLiveData<>();

    public LiveData<String> getLoadingText() {
        return mLoadingText;
    }

    public void setLoadingText(String loadingText) {
        this.mLoadingText.setValue(loadingText);
    }

    private Observer<String> mObserverLoadingText = new Observer<String>() {
        @Override
        public void onChanged(final String loadingText) {
            setLoadingText(loadingText);
        }
    };


    public void observe(LifecycleOwner lifecycleOwner, MediaData mediaData) {
        mediaData.getLoadingProgress().observe(lifecycleOwner, mObserverLoadingProgress);
        mediaData.getLoadingText().observe(lifecycleOwner, mObserverLoadingText);
    }

}

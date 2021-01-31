package com.example.waveplayer.activity_main;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ViewModelActivityMain extends ViewModel {

    private MutableLiveData<Boolean> showFAB;

    public LiveData<Boolean> showFab() {
        return showFAB;
    }

    public void showFab(boolean showFAB){
        this.showFAB.setValue(showFAB);
    }

    private MutableLiveData<String> actionBarTitle;

    public LiveData<String> getActionBarTitle() {
        return actionBarTitle;
    }

    public void setActionBarTitle(String actionBarTitle){
        this.actionBarTitle.setValue(actionBarTitle);
    }

    private MutableLiveData<Integer> fabText;

    public LiveData<Integer> getFABText() {
        return fabText;
    }

    public void setFABText(Integer fabText){
        this.fabText.setValue(fabText);
    }

    private MutableLiveData<Integer> fabImage;

    public LiveData<Integer> getFABImage() {
        return fabImage;
    }

    public void setFabImage(Integer fabImage){
        this.fabImage.setValue(fabImage);
    }

    private MutableLiveData<View.OnClickListener> fabOnClickListener;

    public LiveData<View.OnClickListener> getFabOnClickListener() {
        return fabOnClickListener;
    }

    public void setFabOnClickListener(View.OnClickListener fabOnClickListener){
        this.fabOnClickListener.setValue(fabOnClickListener);
    }
}

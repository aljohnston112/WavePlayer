package com.example.waveplayer.activity_main;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ViewModelActivityMain extends ViewModel {

    private MutableLiveData<Boolean> showFAB;

    public LiveData<Boolean> showFAB() {
        return showFAB;
    }

    public void showFAB(boolean showFAB){
        this.showFAB.setValue(showFAB);
    }

    private MutableLiveData<String> actionBarTitle;

    public LiveData<String> getActionBarTitle() {
        return actionBarTitle;
    }

    public void setActionBarTitle(String actionBarTitle){
        this.actionBarTitle.setValue(actionBarTitle);
    }

    private MutableLiveData<View.OnClickListener> onClickListener;

    public LiveData<View.OnClickListener> getOnClickListener() {
        return onClickListener;
    }

}

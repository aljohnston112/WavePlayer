package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentPaneSong extends Fragment {

    ActivityMain activityMain;

    View view;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    OnLayoutChangeListenerSongPane onLayoutChangeListenerSongPane =
            new OnLayoutChangeListenerSongPane(this);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pane_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        this.view = view;
        if (activityMain != null) {
            if (view.getVisibility() == View.VISIBLE && activityMain.serviceMain != null) {
                activityMain.serviceMain.fragmentSongVisible = false;
            }
            activityMain.updateUI();
        }
        setupSongPane();
        setUpBroadcastReceiver();
    }

    private void setUpBroadcastReceiver() {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notifyServiceConnected();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    public void notifyServiceConnected() {
        if (view.getVisibility() == View.VISIBLE && activityMain.serviceMain != null) {
            activityMain.serviceMain.fragmentSongVisible = false;
        }
        setupSongPane();
        activityMain.updateUI();
    }

    private void setupSongPane() {
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane);
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain = null;
        view.removeOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        onLayoutChangeListenerSongPane = null;
        view = null;
    }

}
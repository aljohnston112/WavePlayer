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

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    OnLayoutChangeListenerSongPane onLayoutChangeListenerSongPane;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pane_song, container, false);
        onLayoutChangeListenerSongPane = new OnLayoutChangeListenerSongPane(this);
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateUI();
        setUpBroadcastReceiver();
    }

    private void updateUI() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        if (view.getVisibility() == View.VISIBLE) {
            activityMain.updateUI();
        }
    }

    private void setUpBroadcastReceiver() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
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
        updateUI();
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        view.removeOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        onLayoutChangeListenerSongPane = null;
    }

}
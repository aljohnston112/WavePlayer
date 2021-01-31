package com.example.waveplayer.fragments.fragment_pane_song;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;

public class FragmentPaneSong extends Fragment {

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private OnLayoutChangeListenerSongPane onLayoutChangeListenerSongPane;

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
        onLayoutChangeListenerSongPane = new OnLayoutChangeListenerSongPane(this);
        view.addOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        updateUI();
        setUpBroadcastReceiver();
    }

    private void updateUI() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        if (view.getVisibility() == View.VISIBLE) {
            activityMain.updateUI();
        }
    }

    private void setUpBroadcastReceiver() {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        view.removeOnLayoutChangeListener(onLayoutChangeListenerSongPane);
        onLayoutChangeListenerSongPane = null;
    }

}
package com.example.waveplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FragmentSelectSongs extends Fragment {

    ActivityMain activityMain;

    View view;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    boolean setUp = false;

    OnClickListenerFABFragmentSelectSongs onClickListenerFABFragmentSelectSongs;

    RecyclerView recyclerViewSongList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        activityMain = ((ActivityMain) getActivity());
        view = inflater.inflate(R.layout.recycler_view_song_list, container, false);
        onClickListenerFABFragmentSelectSongs = new OnClickListenerFABFragmentSelectSongs(this);
        if (activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.select_songs));
        }
        hideKeyboard();
        setupFAB();
        setUpRecyclerView();
        setUpBroadcastReceiverServiceConnected();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(
                Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setupFAB() {
        activityMain.setFabImage(R.drawable.ic_check_white_24dp);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentSelectSongs);
    }

    private void setUpRecyclerView() {
        if (activityMain.serviceMain != null && !setUp) {
            recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
            recyclerViewSongList.setLayoutManager(
                    new LinearLayoutManager(recyclerViewSongList.getContext()));
            for (AudioURI audioURI : activityMain.serviceMain.userPickedSongs) {
                audioURI.setSelected(true);
            }
            RecyclerViewAdapterSelectSongs recyclerViewAdapterSelectSongs =
                    new RecyclerViewAdapterSelectSongs(this);
            recyclerViewSongList.setAdapter(recyclerViewAdapterSelectSongs);
            setUp = true;
        }
    }

    private void setUpBroadcastReceiverServiceConnected() {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpRecyclerView();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerViewSongList.setAdapter(null);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        onClickListenerFABFragmentSelectSongs = null;
        view = null;
        activityMain = null;
    }

}
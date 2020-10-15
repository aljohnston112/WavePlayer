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

import java.util.ArrayList;
import java.util.List;

public class FragmentSelectSongs extends Fragment {

    ActivityMain activityMain;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        setupFAB();
        activityMain.setActionBarTitle(getResources().getString(R.string.select_songs));
        InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        if(activityMain.serviceMain != null) {
            setUpRecyclerView(view);
        }
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction("ServiceConnected");
        activityMain.registerReceiver(new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpRecyclerView(view);
            }
        }, filterComplete);
    }

    private void setUpRecyclerView(View view) {
        RecyclerView recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
        recyclerViewSongList.setLayoutManager(new LinearLayoutManager(recyclerViewSongList.getContext()));
        RecyclerViewAdapterSelectSongs recyclerViewAdapterSelectSongs = new RecyclerViewAdapterSelectSongs(
                this, new ArrayList<>(
                        activityMain.serviceMain.uriMap.values()), activityMain.serviceMain.userPickedSongs);
        recyclerViewSongList.setAdapter(recyclerViewAdapterSelectSongs);
    }

    private void setupFAB() {
        activityMain.setFabImage(R.drawable.ic_check_white_24dp);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.serviceMain.userPickedSongs.clear();
                int nAllSongs = activityMain.serviceMain.uriMap.values().size();
                for(int i = 0; i < nAllSongs; i++) {
                    if (new ArrayList<>(activityMain.serviceMain.uriMap.values()).get(i).isSelected()){
                        activityMain.serviceMain.userPickedSongs.add(
                                new ArrayList<>(activityMain.serviceMain.uriMap.values()).get(i));
                        new ArrayList<>(activityMain.serviceMain.uriMap.values()).get(i).setSelected(false);
                    } else{
                        activityMain.serviceMain.userPickedSongs.remove(
                                new ArrayList<>(activityMain.serviceMain.uriMap.values()).get(i));
                    }
                }
                NavController navController = NavHostFragment.findNavController(FragmentSelectSongs.this);
                navController.popBackStack();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain = null;
    }

}
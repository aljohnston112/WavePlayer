package com.example.waveplayer;

import android.app.Activity;
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        setupFAB();
        activityMain.setActionBarTitle(getResources().getString(R.string.select_songs));
        InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        RecyclerView recyclerViewSongList = activityMain.findViewById(R.id.recycler_view_song_list);
        recyclerViewSongList.setLayoutManager(new LinearLayoutManager(recyclerViewSongList.getContext()));
        RecyclerViewAdapterSelectSongs recyclerViewAdapterSelectSongs = new RecyclerViewAdapterSelectSongs(
                this, activityMain.songs, activityMain.userPickedSongs);
        recyclerViewSongList.setAdapter(recyclerViewAdapterSelectSongs);
    }

    private void setupFAB() {
        activityMain.setFabImage(R.drawable.ic_check_white_24dp);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityMain.userPickedSongs.clear();
                int nAllSongs = activityMain.songs.size();
                for(int i = 0; i < nAllSongs; i++) {
                    if (activityMain.songs.get(i).isSelected()){
                        activityMain.userPickedSongs.add(activityMain.songs.get(i));
                        activityMain.songs.get(i).setSelected(false);
                    } else{
                        activityMain.userPickedSongs.remove(activityMain.songs.get(i));
                    }
                }
                NavController navController = NavHostFragment.findNavController(FragmentSelectSongs.this);
                navController.popBackStack();
            }
        });
    }

}
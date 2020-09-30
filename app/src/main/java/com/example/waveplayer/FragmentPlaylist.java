package com.example.waveplayer;

import android.os.Build;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FragmentPlaylist extends Fragment {

    public FragmentPlaylist() {
        // Required empty public constructor
    }

    public static FragmentPlaylist newInstance() {
        return new FragmentPlaylist();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(activityMain.currentPlaylist.getName());

        activityMain.fab.setOnClickListener(null);
        activityMain.fab.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FragmentPlaylist.this)
                        .navigate(FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
            }
        });

        RecyclerView recyclerView = activityMain.findViewById(R.id.recycler_view_song_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerView.setAdapter(new RecyclerViewAdapterSongs(new ArrayList<>(
                        activityMain.currentPlaylist.getProbFun().getProbMap().keySet()), this));
    }

}
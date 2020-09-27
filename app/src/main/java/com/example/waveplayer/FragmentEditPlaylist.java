package com.example.waveplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Objects;

import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSongs;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentEditPlaylist#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentEditPlaylist extends Fragment {

    ArrayList<AudioURI> songs;
    ArrayList<AudioURI> selectedSongs;

    public FragmentEditPlaylist() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FragmentEditPlaylist.
     */
    public static FragmentEditPlaylist newInstance() {
        FragmentEditPlaylist fragment = new FragmentEditPlaylist();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            RandomPlaylist randomPlaylist = FragmentEditPlaylistArgs.fromBundle(getArguments()).getPlaylist();
            if(randomPlaylist != null) {
                selectedSongs = new ArrayList<>(randomPlaylist.getProbFun().getProbMap().keySet());
            } else {
                selectedSongs = new ArrayList<>();
            }
            songs = FragmentEditPlaylistArgs.fromBundle(getArguments()).getSongList();
        }

        FloatingActionButton fab = Objects.requireNonNull(getActivity()).findViewById(R.id.fab);
        fab.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_check_white_24dp));
        fab.setOnClickListener(null);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
            }
        });
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.edit_playlist);

        view.findViewById(R.id.buttonChangeSongs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentEditPlaylistDirections.ActionFragmentEditPlaylistToFragmentSelectSongs action =
                        FragmentEditPlaylistDirections.actionFragmentEditPlaylistToFragmentSelectSongs(
                                (ArrayList) songs, (ArrayList) selectedSongs);
                NavHostFragment.findNavController(FragmentEditPlaylist.this)
                        .navigate(action);
            }
        });
    }
}
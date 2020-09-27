package com.example.waveplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
    public static final String BUNDLE_KEY_NEW_PLAYLIST = "877";

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

        ArrayList<AudioURI> userPickedSongs = null;
        if (getArguments() != null) {
            RandomPlaylist randomPlaylist = FragmentEditPlaylistArgs.fromBundle(getArguments()).getPlaylist();
            if(randomPlaylist != null) {
                selectedSongs = new ArrayList<>(randomPlaylist.getProbFun().getProbMap().keySet());
            } else {
                selectedSongs = new ArrayList<>();
            }
            songs = FragmentEditPlaylistArgs.fromBundle(getArguments()).getSongList();
            userPickedSongs = (ArrayList<AudioURI>) getArguments().getSerializable(
                    FragmentSelectSongs.BUNDLE_KEY_SELECTED_SONGS);
        }
        if(userPickedSongs != null){
            ((ActivityMain)getActivity()).userPickedSongs = userPickedSongs;
            NavController navController = NavHostFragment.findNavController(FragmentEditPlaylist.this);
            navController.popBackStack();
            navController.popBackStack();
        }
        FloatingActionButton fab = Objects.requireNonNull(getActivity()).findViewById(R.id.fab);
        fab.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_check_white_24dp));
        fab.setOnClickListener(null);
        final ArrayList<AudioURI> finalUserPickedSongs = ((ActivityMain)getActivity()).userPickedSongs;
        final EditText editText = view.findViewById(R.id.editTextPlaylistName);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO put maxPercent in settings
                if(finalUserPickedSongs != null) {
                    RandomPlaylist randomPlaylist = new RandomPlaylist(finalUserPickedSongs, 0.1, editText.toString());
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(BUNDLE_KEY_NEW_PLAYLIST, randomPlaylist);
                    bundle.putSerializable("listPlaylists", null);
                    bundle.putSerializable("listSongs", songs);
                    NavController navController = NavHostFragment.findNavController(FragmentEditPlaylist.this);
                    navController.navigate(R.id.FragmentPlaylists, bundle);
                }
            }
        });
    }
}
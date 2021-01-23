package com.example.waveplayer.fragments.fragment_edit_playlist;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.ViewModelUserPickedSongs;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.List;

public class FragmentEditPlaylist extends Fragment {

    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private ViewModelUserPickedSongs viewModelUserPickedSongs;

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private OnClickListenerFragmentEditPlaylistButtonSelectSongs
            onClickListenerFragmentEditPlaylistButtonSelectSongs;

    private OnClickListenerFABFragmentEditPlaylist onClickListenerFABFragmentEditPlaylist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelUserPickedSongs =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedSongs.class);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.edit_playlist));
        updateFAB();
        onClickListenerFragmentEditPlaylistButtonSelectSongs =
                new OnClickListenerFragmentEditPlaylistButtonSelectSongs(this);
        view.findViewById(R.id.buttonEditSongs).setOnClickListener(
                onClickListenerFragmentEditPlaylistButtonSelectSongs);
        setUpBroadcastReceiverServiceConnected();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateFAB();
    }

    private void updateFAB() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        activityMain.setFABText(R.string.fab_save);
        activityMain.showFab(true);
        final EditText finalEditTextPlaylistName = view.findViewById(R.id.editTextPlaylistName);
        RandomPlaylist userPickedPlaylist = viewModelUserPickedPlaylist.getUserPickedPlaylist();
        List<Song> userPickedSongs = viewModelUserPickedSongs.getUserPickedSongs();
        // userPickedPlaylist is null when user is making a new playlist
        if (userPickedPlaylist != null) {
            // userPickedSongs.isEmpty() when the user is editing a playlist
            if (userPickedSongs.isEmpty()) {
                userPickedSongs.addAll(
                        viewModelUserPickedPlaylist.getUserPickedPlaylist().getSongs());
            }
            finalEditTextPlaylistName.setText(userPickedPlaylist.getName());
        }
        onClickListenerFABFragmentEditPlaylist = new OnClickListenerFABFragmentEditPlaylist(
                this,
                userPickedPlaylist,
                finalEditTextPlaylistName,
                userPickedSongs);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentEditPlaylist);
    }

    private void setUpBroadcastReceiverServiceConnected() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFAB();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        view.findViewById(R.id.buttonEditSongs).setOnClickListener(null);
        onClickListenerFragmentEditPlaylistButtonSelectSongs = null;
        onClickListenerFABFragmentEditPlaylist = null;
    }

    // TODO move?
    void popBackStack() {
        NavController navController = NavHostFragment.findNavController(
                FragmentEditPlaylist.this);
        navController.popBackStack();
    }

}
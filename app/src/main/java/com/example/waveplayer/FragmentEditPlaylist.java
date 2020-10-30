package com.example.waveplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

public class FragmentEditPlaylist extends Fragment {

    ActivityMain activityMain;

    View view;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    OnClickListenerFragmentEditPlaylistButtonSelectSongs
            onClickListenerFragmentEditPlaylistButtonSelectSongs;

    OnClickListenerFABFragmentEditPlaylist onClickListenerFABFragmentEditPlaylist;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_edit_playlist, container, false);
        onClickListenerFragmentEditPlaylistButtonSelectSongs =
                new OnClickListenerFragmentEditPlaylistButtonSelectSongs(this);
        activityMain = ((ActivityMain) getActivity());
        if (activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.edit_playlist));
        }
        updateFAB(view);
        view.findViewById(R.id.buttonEditSongs).setOnClickListener(
                onClickListenerFragmentEditPlaylistButtonSelectSongs);
        setUpBroadcastReceiverServiceConnected(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void updateFAB(View view) {
        activityMain.setFabImage(R.drawable.ic_check_black_24dp);
        activityMain.setFABText(R.string.fab_save);
        activityMain.showFab(true);
        final EditText finalEditTextPlaylistName = view.findViewById(R.id.editTextPlaylistName);
        ArrayList<AudioUri> playlistSongs = new ArrayList<>();
        RandomPlaylist userPickedPlaylist = activityMain.getUserPickedPlaylist();
        // userPickedPlaylist is null when user is making a new playlist
        if (userPickedPlaylist != null) {
            List<AudioUri> userPickedSongs = activityMain.getUserPickedSongs();
            // userPickedSongs.isEmpty() when the user is editing a playlist
            if (userPickedSongs.isEmpty()) {
                userPickedSongs.addAll(userPickedPlaylist.getAudioUris());
            }
            playlistSongs = new ArrayList<>(userPickedPlaylist.getAudioUris());
            finalEditTextPlaylistName.setText(userPickedPlaylist.getName());
        }
        final ArrayList<AudioUri> finalPlaylistSongs = playlistSongs;
        onClickListenerFABFragmentEditPlaylist = new OnClickListenerFABFragmentEditPlaylist(
                userPickedPlaylist, this, finalPlaylistSongs,
                finalEditTextPlaylistName);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentEditPlaylist);
    }

    void popBackStackAndHideKeyboard(View view) {
        NavController navController = NavHostFragment.findNavController(
                FragmentEditPlaylist.this);
        navController.popBackStack();
        InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(
                AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setUpBroadcastReceiverServiceConnected(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateFAB(view);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        view.findViewById(R.id.buttonEditSongs).setOnClickListener(null);
        onClickListenerFragmentEditPlaylistButtonSelectSongs = null;
        onClickListenerFABFragmentEditPlaylist = null;
        activityMain = null;
        view = null;
    }

}
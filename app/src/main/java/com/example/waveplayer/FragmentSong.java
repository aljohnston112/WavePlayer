package com.example.waveplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

public class FragmentSong extends Fragment {

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;
    BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    OnClickListenerFragmentSong onClickListenerFragmentSong;
    OnTouchListenerFragmentSongButtons onTouchListenerFragmentSongButtons;
    OnLayoutChangeListenerFragmentSongButtons onLayoutChangeListenerFragmentSongButtons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.fragmentSongVisible(true);
        activityMain.isSong(true);
        activityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
        activityMain.showFab(false);
        activityMain.setSongToAddToQueue(activityMain.getCurrentSong());
        hideKeyBoard();
        setUpButtons();
        updateUI();
        setUpBroadcastReceiverServiceConnected();
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
    }

    private void updateUI(){
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.updateUI();
    }

    private void hideKeyBoard() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.hideKeyboard(getView());
    }

    private void setUpBroadcastReceiverServiceOnOptionsMenuCreated() {
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        broadcastReceiverOptionsMenuCreated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
                Menu menu = toolbar.getMenu();
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(true);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
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
                notifyServiceConnected();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    public void notifyServiceConnected() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.fragmentSongVisible(true);
        updateUI();
        setUpButtons();
        activityMain.setSongToAddToQueue(activityMain.getCurrentSong());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpButtons() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        final ImageButton buttonBad = view.findViewById(R.id.button_thumb_down);
        final ImageButton buttonGood = view.findViewById(R.id.button_thumb_up);
        final ImageButton buttonShuffle = view.findViewById(R.id.imageButtonShuffle);
        final ImageButton buttonPrev = view.findViewById(R.id.imageButtonPrev);
        final ImageButton buttonPause = view.findViewById(R.id.imageButtonPlayPause);
        final ImageButton buttonNext = view.findViewById(R.id.imageButtonNext);
        final ImageButton buttonLoop = view.findViewById(R.id.imageButtonRepeat);
        onClickListenerFragmentSong = new OnClickListenerFragmentSong(activityMain);
        buttonBad.setOnClickListener(onClickListenerFragmentSong);
        buttonGood.setOnClickListener(onClickListenerFragmentSong);
        buttonShuffle.setOnClickListener(onClickListenerFragmentSong);
        buttonPrev.setOnClickListener(onClickListenerFragmentSong);
        buttonPause.setOnClickListener(onClickListenerFragmentSong);
        buttonNext.setOnClickListener(onClickListenerFragmentSong);
        buttonLoop.setOnClickListener(onClickListenerFragmentSong);
        onTouchListenerFragmentSongButtons = new OnTouchListenerFragmentSongButtons();
        buttonBad.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonGood.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonShuffle.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonPrev.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonPause.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonNext.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonLoop.setOnTouchListener(onTouchListenerFragmentSongButtons);
            if (activityMain.shuffling()) {
                buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp);
            } else {
                buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp);
            }

            if (activityMain.loopingOne()) {
                buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp);
            } else if (activityMain.looping()) {
                buttonLoop.setImageResource(R.drawable.repeat_black_24dp);
            } else {
                buttonLoop.setImageResource(R.drawable.repeat_white_24dp);
            }
        onLayoutChangeListenerFragmentSongButtons =
                new OnLayoutChangeListenerFragmentSongButtons(activityMain);
        view.addOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        final ImageButton buttonBad = view.findViewById(R.id.button_thumb_down);
        final ImageButton buttonGood = view.findViewById(R.id.button_thumb_up);
        final ImageButton buttonShuffle = view.findViewById(R.id.imageButtonShuffle);
        final ImageButton buttonPrev = view.findViewById(R.id.imageButtonPrev);
        final ImageButton buttonPause = view.findViewById(R.id.imageButtonPlayPause);
        final ImageButton buttonNext = view.findViewById(R.id.imageButtonNext);
        final ImageButton buttonLoop = view.findViewById(R.id.imageButtonRepeat);
        buttonBad.setOnClickListener(null);
        buttonGood.setOnClickListener(null);
        buttonShuffle.setOnClickListener(null);
        buttonPrev.setOnClickListener(null);
        buttonPause.setOnClickListener(null);
        buttonNext.setOnClickListener(null);
        buttonLoop.setOnClickListener(null);
        buttonBad.setOnTouchListener(null);
        buttonGood.setOnTouchListener(null);
        buttonShuffle.setOnTouchListener(null);
        buttonPrev.setOnTouchListener(null);
        buttonPause.setOnTouchListener(null);
        buttonNext.setOnTouchListener(null);
        buttonLoop.setOnTouchListener(null);
        onClickListenerFragmentSong = null;
        onTouchListenerFragmentSongButtons = null;
        view.removeOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
        onLayoutChangeListenerFragmentSongButtons = null;
        activityMain.setSongToAddToQueue(null);
    }

}
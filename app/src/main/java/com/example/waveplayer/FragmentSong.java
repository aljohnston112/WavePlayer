package com.example.waveplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class FragmentSong extends Fragment {

    ActivityMain activityMain;

    View view;

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
        view = inflater.inflate(R.layout.fragment_song, container, false);
        activityMain = ((ActivityMain) getActivity());
        if (activityMain != null) {
            if (activityMain.serviceMain != null) {
                activityMain.serviceMain.fragmentSongVisible = true;
            }
            activityMain.isSong = true;
            activityMain.setActionBarTitle(getResources().getString(R.string.now_playing));
            activityMain.showFab(false);
        }
        hideKeyBoard();
        setUpButtons();
        setUpBroadcastReceiverServiceConnected();
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
        onTouchListenerFragmentSongButtons = new OnTouchListenerFragmentSongButtons();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain.updateUI();
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) activityMain.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setUpBroadcastReceiverServiceOnOptionsMenuCreated() {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_on_create_options_menu));
        broadcastReceiverOptionsMenuCreated = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /*
                Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
                Menu menu = toolbar.getMenu();
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(true);

                 */
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
    }

    private void setUpBroadcastReceiverServiceConnected() {
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
        activityMain.serviceMain.fragmentSongVisible = true;
        activityMain.updateUI();
        setUpButtons();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpButtons() {
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
        buttonBad.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonGood.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonShuffle.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonPrev.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonPause.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonNext.setOnTouchListener(onTouchListenerFragmentSongButtons);
        buttonLoop.setOnTouchListener(onTouchListenerFragmentSongButtons);
        if (activityMain.serviceMain != null) {
            if (activityMain.serviceMain.shuffling) {
                buttonShuffle.setImageResource(R.drawable.ic_shuffle_black_24dp);
            } else {
                buttonShuffle.setImageResource(R.drawable.ic_shuffle_white_24dp);
            }

            if (activityMain.serviceMain.loopingOne) {
                buttonLoop.setImageResource(R.drawable.repeat_one_black_24dp);
            } else if (activityMain.serviceMain.looping) {
                buttonLoop.setImageResource(R.drawable.repeat_black_24dp);
            } else {
                buttonLoop.setImageResource(R.drawable.repeat_white_24dp);
            }
        }
        onLayoutChangeListenerFragmentSongButtons =
                new OnLayoutChangeListenerFragmentSongButtons(activityMain);
        view.addOnLayoutChangeListener(onLayoutChangeListenerFragmentSongButtons);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
        view = null;
        activityMain = null;
    }

}
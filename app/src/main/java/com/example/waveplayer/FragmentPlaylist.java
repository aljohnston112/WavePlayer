package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FragmentPlaylist extends Fragment {

    public static final String NAME = "FragmentPlaylist";

    ActivityMain activityMain;

    View view;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    BroadcastReceiver broadcastReceiverOptionsMenuCreated;

    RecyclerView recyclerViewSongList;

    OnQueryTextListenerSearch onQueryTextListenerSearch;
    SongItemTouchListener songItemTouchListener;
    ItemTouchHelper itemTouchHelper;
    OnClickListenerFABFragmentPlaylist onClickListenerFABFragmentPlaylist;
    FragmentPlaylist.UndoListenerSongRemoved undoListenerSongRemoved;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.recycler_view_song_list, container, false);
        activityMain = ((ActivityMain) getActivity());
        if(activityMain != null) {
            activityMain.isSong = false;
        }
        onClickListenerFABFragmentPlaylist = new OnClickListenerFABFragmentPlaylist(this);
        updateFAB();
        setUpUI();
        setUpToolbar();
        setUpBroadCastReceiverOnCompletion();
        setUpBroadcastReceiverServiceOnOptionsMenuCreated();
        hideKeyBoard();
        if (activityMain.serviceMain != null) {
            activityMain.playlistToAddToQueue = activityMain.serviceMain.userPickedPlaylist;
        }
            return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setUpToolbar() {
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            /*
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);

             */
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            if (itemSearch != null) {
                onQueryTextListenerSearch = new OnQueryTextListenerSearch(activityMain, NAME);
                SearchView searchView = (SearchView) (itemSearch.getActionView());
                searchView.setOnQueryTextListener(null);
                searchView.setOnQueryTextListener(onQueryTextListenerSearch);
            }
        }
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager)
                activityMain.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
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
                menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_QUEUE).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_ADD_TO_PLAYLIST_INDEX).setVisible(true);
                menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);

                 */
                setUpToolbar();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOptionsMenuCreated, filterComplete);
    }

    private void setUpBroadCastReceiverOnCompletion() {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpUI();
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void setUpUI() {
        if (activityMain.serviceMain != null) {
            recyclerViewSongList = view.findViewById(R.id.recycler_view_song_list);
            activityMain.setActionBarTitle(activityMain.serviceMain.userPickedPlaylist.getName());
            recyclerViewSongList.setLayoutManager(new LinearLayoutManager(recyclerViewSongList.getContext()));
            RecyclerViewAdapterSongs recyclerViewAdapterSongsList
                    = new RecyclerViewAdapterSongs(this, new ArrayList<>(
                    activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap().keySet()));
            recyclerViewSongList.setAdapter(recyclerViewAdapterSongsList);
            songItemTouchListener = new SongItemTouchListener();
            itemTouchHelper = new ItemTouchHelper(songItemTouchListener);
            itemTouchHelper.attachToRecyclerView(recyclerViewSongList);
        }
    }

    private void updateFAB() {
        activityMain.setFabImage(R.drawable.ic_add_black_24dp);
        activityMain.setFABText(R.string.fab_edit);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(onClickListenerFABFragmentPlaylist);
    }

    class SongItemTouchListener extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            RecyclerViewAdapterSongs recyclerViewAdapterSongsList =
                    (RecyclerViewAdapterSongs) recyclerView.getAdapter();
            if (recyclerViewAdapterSongsList != null) {
                Collections.swap(recyclerViewAdapterSongsList.audioURIS,
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                swapSongPositions(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                recyclerViewAdapterSongsList.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            RecyclerViewAdapterSongs recyclerViewAdapterSongsList =
                    (RecyclerViewAdapterSongs) recyclerViewSongList.getAdapter();
            if (recyclerViewAdapterSongsList != null) {
                int position = viewHolder.getAdapterPosition();
                ProbFun<AudioURI> probFun = activityMain.serviceMain.userPickedPlaylist.getProbFun();
                AudioURI audioURI = recyclerViewAdapterSongsList.audioURIS.get(position);
                double prob = probFun.getProbMap().get(audioURI);
                if(probFun.size() == 1){
                    activityMain.serviceMain.playlists.remove(
                            activityMain.serviceMain.userPickedPlaylist);
                    activityMain.serviceMain.userPickedPlaylist = null;
                } else {
                    probFun.remove(audioURI);
                }
                recyclerViewAdapterSongsList.audioURIS.remove(position);
                recyclerViewAdapterSongsList.notifyItemRemoved(position);
                activityMain.serviceMain.saveFile();
                Snackbar snackbar = Snackbar.make(
                        activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                        R.string.song_removed, BaseTransientBottomBar.LENGTH_LONG);
                undoListenerSongRemoved =
                        new FragmentPlaylist.UndoListenerSongRemoved(recyclerViewAdapterSongsList,
                                audioURI, prob, position);
                snackbar.setAction(R.string.undo, undoListenerSongRemoved);
                snackbar.show();
            }
        }
    }

    private void switchSongPosition(int oldPosition, int newPosition) {
        Map<AudioURI, Double> oldMap =
                activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap();
        ArrayList<AudioURI> keySetList = new ArrayList<>(oldMap.keySet());
        keySetList.add(newPosition, keySetList.get(oldPosition));
        keySetList.remove(oldPosition+1);
        LinkedHashMap<AudioURI, Double> swappedMap = new LinkedHashMap<>();
        for (AudioURI oldSwappedKey : keySetList) {
            swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
        }
        activityMain.serviceMain.userPickedPlaylist.getProbFun().setProbMap(swappedMap);
    }

    private void swapSongPositions(int oldPosition, int newPosition) {
        Map<AudioURI, Double> oldMap =
                activityMain.serviceMain.userPickedPlaylist.getProbFun().getProbMap();
        ArrayList<AudioURI> keySetList = new ArrayList<>(oldMap.keySet());
        Collections.swap(keySetList, oldPosition, newPosition);
        LinkedHashMap<AudioURI, Double> swappedMap = new LinkedHashMap<>();
        for (AudioURI oldSwappedKey : keySetList) {
            swappedMap.put(oldSwappedKey, oldMap.get(oldSwappedKey));
        }
        activityMain.serviceMain.userPickedPlaylist.getProbFun().setProbMap(swappedMap);
    }

    public class UndoListenerSongRemoved implements View.OnClickListener {

        RecyclerViewAdapterSongs recyclerViewAdapter;

        AudioURI audioURI;

        double probability;

        int position;

        UndoListenerSongRemoved(RecyclerViewAdapterSongs recyclerViewAdapter, AudioURI audioURI,
                                double probability, int position) {
            this.recyclerViewAdapter = recyclerViewAdapter;
            this.audioURI = audioURI;
            this.probability = probability;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            activityMain.serviceMain.userPickedPlaylist.getProbFun().add(audioURI, probability);
            switchSongPosition(
                    activityMain.serviceMain.userPickedPlaylist.getProbFun().probMap.keySet().size()-1,
                    position);
            recyclerViewAdapter.updateList(new ArrayList<>(
                    activityMain.serviceMain.userPickedPlaylist.getProbFun().probMap.keySet()));
            recyclerViewAdapter.notifyDataSetChanged();
            activityMain.serviceMain.saveFile();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerViewSongList.setAdapter(null);
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        activityMain.unregisterReceiver(broadcastReceiverOptionsMenuCreated);
        broadcastReceiverOptionsMenuCreated = null;
        onQueryTextListenerSearch = null;
        Toolbar toolbar = activityMain.findViewById(R.id.toolbar);
        Menu menu = toolbar.getMenu();
        if (menu != null) {
            /*
            menu.getItem(ActivityMain.MENU_ACTION_RESET_PROBS_INDEX).setVisible(true);
            menu.getItem(ActivityMain.MENU_ACTION_SEARCH_INDEX).setVisible(true);

             */
            MenuItem itemSearch = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) (itemSearch.getActionView());
            searchView.onActionViewCollapsed();
        }
        itemTouchHelper.attachToRecyclerView(null);
        songItemTouchListener = null;
        itemTouchHelper = null;
        onClickListenerFABFragmentPlaylist = null;
        activityMain.playlistToAddToQueue = null;
        activityMain = null;
        view = null;
    }

}
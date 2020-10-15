package com.example.waveplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;

public class FragmentPlaylists extends Fragment {

    ActivityMain activityMain;

    RecyclerView recyclerView;

    RecyclerViewAdapterPlaylists recyclerViewAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        updateMainContent(view);
    }

    private void updateMainContent(final View view) {
        updateFAB();
        activityMain.setActionBarTitle(getResources().getString(R.string.playlists));
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
        recyclerView = view.findViewById(R.id.recycler_view_playlist_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        recyclerViewAdapter = new RecyclerViewAdapterPlaylists(this, activityMain.serviceMain.playlists);
        recyclerView.setAdapter(recyclerViewAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new PlaylistItemTouchListener());
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void updateFAB() {
        activityMain.setFabImage(R.drawable.ic_add_black_24dp);
        activityMain.showFab(true);
        activityMain.setFabOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                activityMain.serviceMain.userPickedPlaylist = null;
                activityMain.serviceMain.userPickedSongs.clear();
                NavHostFragment.findNavController(FragmentPlaylists.this)
                        .navigate(FragmentPlaylistsDirections.actionFragmentPlaylistsToFragmentEditPlaylist());
            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getGroupId()) {
            case RecyclerViewAdapterPlaylists.MENU_DELETE_PLAYLIST_GROUP_ID:
                activityMain.serviceMain.playlists.remove(item.getItemId());
                recyclerViewAdapter.notifyItemRemoved(item.getItemId());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    class PlaylistItemTouchListener extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            Collections.swap(recyclerViewAdapter.randomPlaylists, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            Collections.swap(activityMain.serviceMain.playlists, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            recyclerViewAdapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain = null;
        recyclerViewAdapter = null;
        recyclerView = null;
    }

}
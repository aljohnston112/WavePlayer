package com.example.waveplayer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
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

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;

public class FragmentPlaylists extends Fragment {

    ActivityMain activityMain;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recycler_view_playlist_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        updateMainContent(view);
    }

    private void updateMainContent(final View view) {
        activityMain.setActionBarTitle(getResources().getString(R.string.playlists));
        setUpRecyclerView(view);
        setUpBroadcastReceiver(view);
        updateFAB();
    }

    private void setUpBroadcastReceiver(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpRecyclerView(view);
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void setUpRecyclerView(View view) {
        if (activityMain.serviceMain != null) {
            RecyclerView recyclerView = view.findViewById(R.id.recycler_view_playlist_list);
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
            RecyclerViewAdapterPlaylists recyclerViewAdapter =
                    new RecyclerViewAdapterPlaylists(
                            this, activityMain.serviceMain.playlists);
            recyclerView.setAdapter(recyclerViewAdapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new PlaylistItemTouchListener());
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }
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

    public class UndoListener implements View.OnClickListener {

        RecyclerViewAdapterPlaylists recyclerViewAdapter;

        RandomPlaylistTreeMap randomPlaylistTreeMap;

        UndoListener(RecyclerViewAdapterPlaylists recyclerViewAdapter, RandomPlaylistTreeMap randomPlaylistTreeMap) {
            this.recyclerViewAdapter = recyclerViewAdapter;
            this.randomPlaylistTreeMap = randomPlaylistTreeMap;
        }

        @Override
        public void onClick(View v) {
            activityMain.serviceMain.playlists.add(randomPlaylistTreeMap);
            recyclerViewAdapter.notifyDataSetChanged();
            activityMain.serviceMain.saveFile();
        }
    }

    class PlaylistItemTouchListener extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            RecyclerViewAdapterPlaylists recyclerViewAdapter =
                    (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
            if (recyclerViewAdapter != null) {
                Collections.swap(recyclerViewAdapter.randomPlaylistTreeMaps,
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                Collections.swap(activityMain.serviceMain.playlists,
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
                recyclerViewAdapter.notifyItemMoved(
                        viewHolder.getAdapterPosition(), target.getAdapterPosition());
            }
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            RecyclerView recyclerView = activityMain.findViewById(R.id.recycler_view_playlist_list);
            RecyclerViewAdapterPlaylists recyclerViewAdapter =
                    (RecyclerViewAdapterPlaylists) recyclerView.getAdapter();
            if (recyclerViewAdapter != null) {
                int position = viewHolder.getAdapterPosition();
                RandomPlaylistTreeMap randomPlaylistTreeMap = activityMain.serviceMain.playlists.get(position);
                activityMain.serviceMain.playlists.remove(position);
                recyclerViewAdapter.notifyItemRemoved(position);
                activityMain.serviceMain.saveFile();
                Snackbar snackbar = Snackbar.make(
                        activityMain.findViewById(R.id.coordinatorLayoutActivityMain),
                        R.string.playlist_deleted, BaseTransientBottomBar.LENGTH_LONG);
                snackbar.setAction(R.string.undo,
                        new UndoListener(recyclerViewAdapter, randomPlaylistTreeMap));
                snackbar.show();
            }
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain = null;
    }

}
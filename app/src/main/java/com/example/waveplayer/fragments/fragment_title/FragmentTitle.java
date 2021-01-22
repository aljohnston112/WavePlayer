package com.example.waveplayer.fragments.fragment_title;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.ActivityMain;
import com.example.waveplayer.Song;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.random_playlist.AudioUri;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;

public class FragmentTitle extends Fragment {

    public static final int REQUEST_CODE_OPEN_FOLDER = 9367;

    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    private OnClickListenerFragmentTitleButtons onClickListenerFragmentTitleButtons;

    private Uri uriUserPickedFolder;

    private long mediaStoreUriID;

    private List<Song> songs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_title, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        songs = new ArrayList<>();
        updateMainContent();
        setUpButtons();
        setUpBroadCastReceiver();
    }

    private void setUpBroadCastReceiver() {
        final ActivityMain activityMain = ((ActivityMain) getActivity());
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (uriUserPickedFolder != null) {
                    if (!songs.isEmpty()) {
                        RandomPlaylist randomPlaylist =
                                activityMain.getPlaylist(uriUserPickedFolder.getPath());
                        if (randomPlaylist == null) {
                            randomPlaylist = new RandomPlaylist(
                                    uriUserPickedFolder.getPath(), songs, activityMain.getMaxPercent(),
                                    false, mediaStoreUriID);
                            activityMain.addPlaylist(randomPlaylist);
                        } else {
                            addNewSongs(randomPlaylist);
                            removeMissingSongs(randomPlaylist);
                        }
                        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
                    }
                    activityMain.saveFile();
                    NavHostFragment.findNavController(FragmentTitle.this)
                            .navigate(actionFragmentTitleToFragmentPlaylists());
                }
            }

            private void removeMissingSongs(RandomPlaylist randomPlaylist) {
                for (Song song : randomPlaylist.getSongs()) {
                    if (!songs.contains(song)) {
                        randomPlaylist.remove(song);
                        songs.remove(song);
                    }
                }
            }

            private void addNewSongs(RandomPlaylist randomPlaylist) {
                for (Song song : songs) {
                    if (song != null) {
                        if (!randomPlaylist.contains(song)) {
                            randomPlaylist.add(song);
                        }
                    }
                }
            }

        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }

    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.app_name));
        activityMain.showFab(false);
    }

    private void setUpButtons() {
        View view = getView();
        onClickListenerFragmentTitleButtons = new OnClickListenerFragmentTitleButtons(this);
        view.findViewById(R.id.button_playlists).setOnClickListener(onClickListenerFragmentTitleButtons);
        view.findViewById(R.id.button_songs).setOnClickListener(onClickListenerFragmentTitleButtons);
        view.findViewById(R.id.button_settings).setOnClickListener(onClickListenerFragmentTitleButtons);
        view.findViewById(R.id.button_folder_search).setOnClickListener(onClickListenerFragmentTitleButtons);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == REQUEST_CODE_OPEN_FOLDER && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                this.uriUserPickedFolder = uri;
                getFilesFromDirRecursive(uri);
            }
        }
    }

    void getFilesFromDirRecursive(Uri rootUri) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                rootUri, DocumentsContract.getTreeDocumentId(rootUri));
        getFiles(childrenUri, rootUri);
    }

    private void getFiles(Uri childrenUri, Uri rootUri) {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        ContentResolver contentResolver = activityMain.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? OR" +
                DocumentsContract.Document.COLUMN_MIME_TYPE + " == ?";
        String[] selectionArgs = new String[]{"0", DocumentsContract.Document.MIME_TYPE_DIR};
        try (Cursor cursor = contentResolver.query(childrenUri, new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST_ID,
                        MediaStore.Audio.Media.DATA},
                selection, selectionArgs, null)) {
            if (cursor != null) {
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String mime = cursor.getString(1);
                    String displayName = cursor.getString(nameCol);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                        Uri newNode = DocumentsContract.buildChildDocumentsUriUsingTree(
                                rootUri, docId);
                        getFiles(newNode, rootUri);
                    } else {
                        songs.add(getSong(displayName));
                    }
                }
            }
        }
    }

    private Song getSong(String displayName) {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        ContentResolver contentResolver = activityMain.getContentResolver();
        String selection = MediaStore.Audio.Media.DISPLAY_NAME + " == ?";
        String[] selectionArgs = new String[]{displayName};
        try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST_ID
                },
                selection, selectionArgs, null)) {
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
                    long id = cursor.getLong(idCol);
                    this.mediaStoreUriID = id;
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    return new Song(id, title);
                }
            }
        }
        return null;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = ((ActivityMain) getActivity());
        View view = getView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        uriUserPickedFolder = null;
        for (int i = 0; i < songs.size(); i++) {
            songs.set(i, null);
        }
        songs = null;
        view.findViewById(R.id.button_playlists).setOnClickListener(null);
        view.findViewById(R.id.button_songs).setOnClickListener(null);
        view.findViewById(R.id.button_settings).setOnClickListener(null);
        view.findViewById(R.id.button_folder_search).setOnClickListener(null);
        onClickListenerFragmentTitleButtons = null;
    }

}
package com.example.waveplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylist;

public class FragmentTitle extends Fragment {

    public static final int REQUEST_CODE_OPEN_FOLDER = 9367;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    OnClickListenerFragmentTitleButtons onClickListenerFragmentTitleButtons;

    Uri uriUserPicked;

    long mediaStoreUriID;

    List<AudioUri> audioUris;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        audioUris = new ArrayList<>();
        return inflater.inflate(R.layout.fragment_title, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
                if (uriUserPicked != null) {
                    if (!audioUris.isEmpty()) {
                        if (!activityMain.containsDirectoryPlaylists(mediaStoreUriID)) {
                            RandomPlaylist randomPlaylist = new RandomPlaylist(
                                    uriUserPicked.getPath(), audioUris, activityMain.getMaxPercent(),
                                    false, mediaStoreUriID);
                            activityMain.addDirectoryPlaylist(mediaStoreUriID, randomPlaylist);
                        } else {
                            RandomPlaylist randomPlaylist =
                                    activityMain.getDirectoryPlaylist(mediaStoreUriID);
                            addNewSongs(randomPlaylist);
                            removeMissingSongs(randomPlaylist);
                        }
                        activityMain.setUserPickedPlaylist(
                                activityMain.getDirectoryPlaylist(mediaStoreUriID));
                    }
                    NavHostFragment.findNavController(FragmentTitle.this)
                            .navigate(actionFragmentTitleToFragmentPlaylist());
                }
            }

            private void removeMissingSongs(RandomPlaylist randomPlaylist) {
                for (AudioUri audioURI : randomPlaylist.getAudioUris()) {
                    if (!audioUris.contains(audioURI)) {
                        randomPlaylist.remove(audioURI);
                        audioUris.remove(audioURI);
                    }
                }
            }

            private void addNewSongs(RandomPlaylist randomPlaylist) {
                for (AudioUri audioURIFromUriMap : audioUris) {
                    if (audioURIFromUriMap != null) {
                        if (!randomPlaylist.contains(audioURIFromUriMap)) {
                            randomPlaylist.add(audioURIFromUriMap);
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
                this.uriUserPicked = uri;
                traverseDirectoryEntries(uri);
            }
        }
    }

    void traverseDirectoryEntries(Uri rootUri) {
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
                    if (isDirectory(mime)) {
                        Uri newNode = DocumentsContract.buildChildDocumentsUriUsingTree(
                                rootUri, docId);
                        getFiles(newNode, rootUri);
                    } else {
                        audioUris.add(getAudioUri(displayName));
                    }
                }
            }
        }
    }

    private static boolean isDirectory(String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }

    private AudioUri getAudioUri(String displayName) {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        ContentResolver contentResolver = activityMain.getContentResolver();
        String selection = MediaStore.Audio.Media.DISPLAY_NAME + " == ?";
        String[] selectionArgs = new String[]{displayName};
        try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST_ID,
                        MediaStore.Audio.Media.DATA
                },
                selection, selectionArgs, null)) {
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    long id = cursor.getLong(idCol);
                    this.mediaStoreUriID = id;
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String data = cursor.getString(dataCol);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    return new AudioUri(uri, data, displayName, artist, title, id);
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
        uriUserPicked = null;
        for (int i = 0; i < audioUris.size(); i++) {
            audioUris.set(i, null);
        }
        audioUris = null;
        view.findViewById(R.id.button_playlists).setOnClickListener(null);
        view.findViewById(R.id.button_songs).setOnClickListener(null);
        view.findViewById(R.id.button_settings).setOnClickListener(null);
        view.findViewById(R.id.button_folder_search).setOnClickListener(null);
        onClickListenerFragmentTitleButtons = null;
    }

}
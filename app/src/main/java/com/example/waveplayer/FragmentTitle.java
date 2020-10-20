package com.example.waveplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylist;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSettings;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSongs;

public class FragmentTitle extends Fragment {

    ActivityMain activityMain;

    BroadcastReceiverOnServiceConnected broadcastReceiverOnServiceConnected;

    Uri uri;

    long mediaStoreUriID;

    List<Uri> audioFiles = new ArrayList<>();

    List<AudioURI> audioURIs = new ArrayList<>();

    private static final int REQUEST_CODE_OPEN_FOLDER = 9367;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_title, container, false);
    }

    private void setUpBroadCastReceiver(final View view) {
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addCategory(Intent.CATEGORY_DEFAULT);
        filterComplete.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (uri != null) {
                    if (!audioURIs.isEmpty()) {
                        if(activityMain.serviceMain.directoryPlaylists.get(mediaStoreUriID) == null) {
                            activityMain.serviceMain.directoryPlaylists.put(
                                    mediaStoreUriID, new RandomPlaylist(audioURIs, ServiceMain.MAX_PERCENT, uri.getPath()));
                        } else{
                            //TODO
                        }
                        activityMain.serviceMain.userPickedPlaylist =
                                activityMain.serviceMain.directoryPlaylists.get(mediaStoreUriID);
                    }
                    NavHostFragment.findNavController(FragmentTitle.this)
                            .navigate(actionFragmentTitleToFragmentPlaylist());
                }
            }
        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, filterComplete);
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        updateMainContent();
        setUpButtons(view);
        setUpBroadCastReceiver(view);
    }

    private void updateMainContent() {
        if (activityMain != null) {
            activityMain.setActionBarTitle(getResources().getString(R.string.app_name));
            activityMain.showFab(false);
        }
    }

    private void setUpButtons(View view) {
        view.findViewById(R.id.button_playlists).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FragmentTitle.this)
                        .navigate(actionFragmentTitleToFragmentPlaylists());
            }
        });
        view.findViewById(R.id.button_songs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FragmentTitle.this)
                        .navigate(actionFragmentTitleToFragmentSongs());
            }
        });
        view.findViewById(R.id.button_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FragmentTitle.this)
                        .navigate(actionFragmentTitleToFragmentSettings());
            }
        });
        view.findViewById(R.id.button_folder_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String title = getResources().getString(R.string.pick_folder);
                Intent chooser = Intent.createChooser(intent, title);
                if (intent.resolveActivity(activityMain.getPackageManager()) != null) {
                    startActivityForResult(chooser, REQUEST_CODE_OPEN_FOLDER);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == REQUEST_CODE_OPEN_FOLDER && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ContentResolver contentResolver = activityMain.getContentResolver();
                contentResolver.takePersistableUriPermission(uri, takeFlags);
                this.uri = uri;
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
/*
        MediaMetadataRetriever mediaMetadataRetriever;
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(activityMain.getApplicationContext(), MediaStore.getMediaUri(childrenUri));
        try {
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        } catch (Exception e) {
            // TO-DO Exception
        }
*/

        ContentResolver contentResolver = activityMain.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? OR" +
                DocumentsContract.Document.COLUMN_MIME_TYPE + " == ?";
        String[] selectionArgs = new String[]{"0", DocumentsContract.Document.MIME_TYPE_DIR};
        try (Cursor cursor = contentResolver.query(childrenUri, new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA},
                selection, selectionArgs, null)) {
            if (cursor != null) {
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String name = cursor.getString(1);
                    String mime = cursor.getString(2);
                    String displayName = cursor.getString(nameCol);
                    if (isDirectory(mime)) {
                        Uri newNode = DocumentsContract.buildChildDocumentsUriUsingTree(
                                rootUri, docId);
                        getFiles( newNode, rootUri);
                    } else {
                        audioFiles.add(childrenUri);
                        audioURIs.add(getAudioUri(displayName));
                    }
                }
            }
        }
    }

    // Util method to check if the mime type is a directory
    private static boolean isDirectory(String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }

    private AudioURI getAudioUri(String displayName) {
        ContentResolver contentResolver = activityMain.getContentResolver();
        String selection =  MediaStore.Audio.Media.DISPLAY_NAME + " == ?";
        String[] selectionArgs = new String[]{displayName};
        try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.DATA
                },
                selection, selectionArgs, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    long id = cursor.getLong(idCol);
                    this.mediaStoreUriID = id;
                    String displayName2 = cursor.getString(nameCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String data = cursor.getString(dataCol);
                    Uri uri1 = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    return new AudioURI(uri1, data, displayName2, artist, title, id);
                }
            }
        }
        return null;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        activityMain = null;
    }

}
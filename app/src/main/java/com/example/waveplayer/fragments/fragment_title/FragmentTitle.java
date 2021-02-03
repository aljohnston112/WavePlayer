package com.example.waveplayer.fragments.fragment_title;

import android.app.Activity;
import android.content.BroadcastReceiver;
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

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.activity_main.ViewModelActivityMain;
import com.example.waveplayer.databinding.FragmentTitleBinding;
import com.example.waveplayer.media_controller.SaveFile;
import com.example.waveplayer.random_playlist.Song;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;
import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentSettings;
import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentSongs;

public class FragmentTitle extends Fragment {

    public static final int REQUEST_CODE_OPEN_FOLDER = 9367;

    private ViewModelActivityMain viewModelActivityMain;

    private FragmentTitleBinding binding;

    private ViewModelUserPickedPlaylist viewModelUserPickedPlaylist;

    private BroadcastReceiver broadcastReceiverOnServiceConnected;

    private View.OnClickListener onClickListenerFragmentTitleButtons;

    private Uri uriUserPickedFolder;

    private List<Song> songs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTitleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelUserPickedPlaylist =
                new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        viewModelActivityMain =
                new ViewModelProvider(requireActivity()).get(ViewModelActivityMain.class);
        songs = new ArrayList<>();
        updateMainContent();
        setUpButtons();
        setUpBroadCastReceiver();
    }

    private void updateMainContent() {
        viewModelActivityMain.setActionBarTitle(getResources().getString(R.string.app_name));
        viewModelActivityMain.showFab(false);
    }

    private void setUpButtons() {
        onClickListenerFragmentTitleButtons = view -> {
            if (view.getId() == R.id.button_playlists) {
                NavHostFragment.findNavController(this)
                        .navigate(actionFragmentTitleToFragmentPlaylists());
            } else if (view.getId() == R.id.button_songs) {
                NavHostFragment.findNavController(this)
                        .navigate(actionFragmentTitleToFragmentSongs());
            } else if (view.getId() == R.id.button_settings) {
                NavHostFragment.findNavController(this)
                        .navigate(actionFragmentTitleToFragmentSettings());
            } else if (view.getId() == R.id.button_folder_search) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String title = view.getResources().getString(R.string.pick_folder);
                Intent chooser = Intent.createChooser(intent, title);
                startActivityForResult(chooser, FragmentTitle.REQUEST_CODE_OPEN_FOLDER);
            }
        };
        binding.buttonPlaylists.setOnClickListener(onClickListenerFragmentTitleButtons);
        binding.buttonSongs.setOnClickListener(onClickListenerFragmentTitleButtons);
        binding.buttonSettings.setOnClickListener(onClickListenerFragmentTitleButtons);
        binding.buttonFolderSearch.setOnClickListener(onClickListenerFragmentTitleButtons);
    }


    // TODO possibly delete
    private void setUpBroadCastReceiver() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter intentFilterServiceConnected = new IntentFilter();
        intentFilterServiceConnected.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilterServiceConnected.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        broadcastReceiverOnServiceConnected = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }

        };
        activityMain.registerReceiver(broadcastReceiverOnServiceConnected, intentFilterServiceConnected);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        if (requestCode == REQUEST_CODE_OPEN_FOLDER && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                this.uriUserPickedFolder = uri;
                getFilesFromDirRecursive(uri);
                if (uriUserPickedFolder != null) {
                    if (!songs.isEmpty()) {
                        RandomPlaylist randomPlaylist =
                                activityMain.getPlaylist(uriUserPickedFolder.getPath());
                        if (randomPlaylist == null) {
                            randomPlaylist = new RandomPlaylist(
                                    uriUserPickedFolder.getPath(), songs, activityMain.getMaxPercent(),
                                    false);
                            activityMain.addPlaylist(randomPlaylist);
                        } else {
                            addNewSongs(randomPlaylist);
                            removeMissingSongs(randomPlaylist);
                        }
                        viewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
                    }
                    SaveFile.saveFile(requireActivity().getApplicationContext());
                    NavHostFragment.findNavController(FragmentTitle.this)
                            .navigate(actionFragmentTitleToFragmentPlaylists());
                }
            }
        }
    }

    void getFilesFromDirRecursive(Uri rootUri) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                rootUri, DocumentsContract.getTreeDocumentId(rootUri));
        getFiles(childrenUri, rootUri);
    }

    private void getFiles(Uri childrenUri, Uri rootUri) {
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ContentResolver contentResolver = activityMain.getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != ? OR" + DocumentsContract.Document.COLUMN_MIME_TYPE + " == ?";
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
        ActivityMain activityMain = (ActivityMain) requireActivity();
        ContentResolver contentResolver = activityMain.getContentResolver();
        String selection = MediaStore.Audio.Media.DISPLAY_NAME + " == ?";
        String[] selectionArgs = new String[]{displayName};
        try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                },
                selection, selectionArgs, null)) {
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    return new Song(id, title);
                }
            }
        }
        return null;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActivityMain activityMain = (ActivityMain) requireActivity();
        activityMain.unregisterReceiver(broadcastReceiverOnServiceConnected);
        broadcastReceiverOnServiceConnected = null;
        binding.buttonPlaylists.setOnClickListener(null);
        binding.buttonSongs.setOnClickListener(null);
        binding.buttonSettings.setOnClickListener(null);
        binding.buttonFolderSearch.setOnClickListener(null);
        onClickListenerFragmentTitleButtons = null;
        binding = null;
        uriUserPickedFolder = null;
        for (int i = 0; i < songs.size(); i++) {
            songs.set(i, null);
        }
        songs = null;
        viewModelUserPickedPlaylist = null;
        viewModelActivityMain = null;
    }

}
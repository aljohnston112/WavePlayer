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

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.databinding.FragmentTitleBinding;
import com.example.waveplayer.media_controller.MediaData;
import com.example.waveplayer.media_controller.SaveFile;
import com.example.waveplayer.media_controller.Song;
import com.example.waveplayer.ViewModelUserPickedPlaylist;
import com.example.waveplayer.fragments.BroadcastReceiverOnServiceConnected;
import com.example.waveplayer.R;
import com.example.waveplayer.random_playlist.RandomPlaylist;

import java.util.ArrayList;
import java.util.List;

import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;

public class FragmentTitle extends Fragment {

    public static final int REQUEST_CODE_OPEN_FOLDER = 9367;

    private FragmentTitleBinding mBinding;

    private ViewModelUserPickedPlaylist mViewModelUserPickedPlaylist;

    private BroadcastReceiverOnServiceConnected mBroadcastReceiverOnServiceConnected;

    private OnClickListenerFragmentTitleButtons mOnClickListenerFragmentTitleButtons;

    private Uri mUriUserPickedFolder;

    private List<Song> mSongs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentTitleBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModelUserPickedPlaylist = new ViewModelProvider(requireActivity()).get(ViewModelUserPickedPlaylist.class);
        mSongs = new ArrayList<>();
        updateMainContent();
        setUpButtons();
        setUpBroadCastReceiver();
    }

    private void updateMainContent() {
        ActivityMain activityMain = ((ActivityMain) getActivity());
        activityMain.setActionBarTitle(getResources().getString(R.string.app_name));
        activityMain.showFab(false);
    }

    private void setUpButtons() {
        mOnClickListenerFragmentTitleButtons = new OnClickListenerFragmentTitleButtons(this);
        mBinding.buttonPlaylists.setOnClickListener(mOnClickListenerFragmentTitleButtons);
        mBinding.buttonSongs.setOnClickListener(mOnClickListenerFragmentTitleButtons);
        mBinding.buttonSettings.setOnClickListener(mOnClickListenerFragmentTitleButtons);
        mBinding.buttonFolderSearch.setOnClickListener(mOnClickListenerFragmentTitleButtons);
    }


    // TODO possibly delete
    private void setUpBroadCastReceiver() {
        final ActivityMain activityMain = (ActivityMain) requireActivity();
        IntentFilter intentFilterServiceConnected = new IntentFilter();
        intentFilterServiceConnected.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilterServiceConnected.addAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        mBroadcastReceiverOnServiceConnected = new BroadcastReceiverOnServiceConnected() {
            @Override
            public void onReceive(Context context, Intent intent) {

            }

        };
        activityMain.registerReceiver(mBroadcastReceiverOnServiceConnected, intentFilterServiceConnected);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == REQUEST_CODE_OPEN_FOLDER && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();
                this.mUriUserPickedFolder = uri;
                getFilesFromDirRecursive(uri);
                MediaData mediaData = MediaData.getInstance();
                if (mUriUserPickedFolder != null) {
                    if (!mSongs.isEmpty()) {
                        RandomPlaylist randomPlaylist =
                                MediaData.getInstance().getPlaylist(mUriUserPickedFolder.getPath());
                        if (randomPlaylist == null) {
                            randomPlaylist = new RandomPlaylist(
                                    mUriUserPickedFolder.getPath(), mSongs, mediaData.getMaxPercent(),
                                    false);
                            mediaData.addPlaylist(randomPlaylist);
                        } else {
                            addNewSongs(randomPlaylist);
                            removeMissingSongs(randomPlaylist);
                        }
                        mViewModelUserPickedPlaylist.setUserPickedPlaylist(randomPlaylist);
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
                        mSongs.add(getSong(displayName));
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
            if (!mSongs.contains(song)) {
                randomPlaylist.remove(song);
                mSongs.remove(song);
            }
        }
    }

    private void addNewSongs(RandomPlaylist randomPlaylist) {
        for (Song song : mSongs) {
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
        activityMain.unregisterReceiver(mBroadcastReceiverOnServiceConnected);
        mBroadcastReceiverOnServiceConnected = null;
        mBinding.buttonPlaylists.setOnClickListener(null);
        mBinding.buttonSongs.setOnClickListener(null);
        mBinding.buttonSettings.setOnClickListener(null);
        mBinding.buttonFolderSearch.setOnClickListener(null);
        mOnClickListenerFragmentTitleButtons = null;
        mBinding = null;
        mUriUserPickedFolder = null;
        for (int i = 0; i < mSongs.size(); i++) {
            mSongs.set(i, null);
        }
        mSongs = null;
        mViewModelUserPickedPlaylist = null;
    }

}
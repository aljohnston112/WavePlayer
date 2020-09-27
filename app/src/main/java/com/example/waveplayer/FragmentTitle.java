package com.example.waveplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSongs;

public class FragmentTitle extends Fragment {

    private static final int REQUEST_PERMISSION = 2;

    List<AudioURI> audioList = new ArrayList<>();

    List<RandomPlaylist> randomPlaylists = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Objects.requireNonNull(getActivity()).checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                try {
                    getWaveFiles();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                getWaveFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
                /*
                Intent folderOpenIntent = new Intent();
                folderOpenIntent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);
                if(folderOpenIntent.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(folderOpenIntent, OPEN_FOLDER);
                }
                 */
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        FloatingActionButton fab = Objects.requireNonNull(getActivity()).findViewById(R.id.fab);
        fab.hide();
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_title, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        view.findViewById(R.id.button_playlists).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTitleDirections.ActionFragmentTitleToFragmentPlaylists action =
                        actionFragmentTitleToFragmentPlaylists((ArrayList) audioList, (ArrayList) randomPlaylists);
                NavHostFragment.findNavController(FragmentTitle.this)
                        .navigate(action);
            }
        });

        view.findViewById(R.id.button_songs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTitleDirections.ActionFragmentTitleToFragmentSongs action =
                        actionFragmentTitleToFragmentSongs((ArrayList) audioList);
                NavHostFragment.findNavController(FragmentTitle.this)
                        .navigate(action);
            }
        });

        view.findViewById(R.id.button_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FragmentTitle.this)
                        .navigate(R.id.action_FragmentTitle_to_FragmentSettings);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                getWaveFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getWaveFiles() throws IOException {
        String[] projection = new String[]{MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.COMPOSER,
                MediaStore.Audio.Media.TRACK};
        /*
        String selection = MediaStore.Audio.Media.MIME_TYPE +
                " == ?";
        String[] selectionArgs = new String[] {
                "audio/x-wav"
        };
         */
        String selection = MediaStore.Audio.Media.IS_MUSIC +
                " != ?";
        String[] selectionArgs = new String[]{
                "0"
        };
        String sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC";
        try (Cursor cursor = Objects.requireNonNull(getActivity()).getApplicationContext().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null) {
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    Bitmap thumbnail =
                            null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        thumbnail = getActivity().getApplicationContext().getContentResolver().loadThumbnail(
                                contentUri, new Size(640, 480), null);
                    }
                    audioList.add(new AudioURI(contentUri, thumbnail, artist, title));
                }
            }
        }
        // playWaveFile();
    }

    private void playWaveFile() throws IOException {
        Uri myUri = audioList.get(0).uri;
        MediaPlayer mediaPlayer = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
        }
        mediaPlayer.setDataSource(Objects.requireNonNull(getActivity()).getApplicationContext(), myUri);
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

}
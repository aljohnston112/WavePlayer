package com.example.waveplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.navigation.fragment.NavHostFragment;
import java.io.IOException;
import java.util.Objects;

import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSettings;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSongs;

public class FragmentTitle extends Fragment {

    ActivityMain activityMain;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_title, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        activityMain.actionBar.setTitle(R.string.app_name);
        activityMain.fab.hide();

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

    }

}
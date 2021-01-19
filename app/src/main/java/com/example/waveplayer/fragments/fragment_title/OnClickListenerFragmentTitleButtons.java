package com.example.waveplayer.fragments.fragment_title;

import android.content.Intent;
import android.view.View;

import androidx.navigation.fragment.NavHostFragment;

import com.example.waveplayer.R;

import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;
import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentSettings;
import static com.example.waveplayer.fragments.fragment_title.FragmentTitleDirections.actionFragmentTitleToFragmentSongs;

public class OnClickListenerFragmentTitleButtons implements View.OnClickListener {

    private final FragmentTitle fragmentTitle;

    OnClickListenerFragmentTitleButtons(FragmentTitle fragmentTitle) {
        this.fragmentTitle = fragmentTitle;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button_playlists) {
            NavHostFragment.findNavController(fragmentTitle)
                    .navigate(actionFragmentTitleToFragmentPlaylists());
        } else if (view.getId() == R.id.button_songs) {
            NavHostFragment.findNavController(fragmentTitle)
                    .navigate(actionFragmentTitleToFragmentSongs());
        } else if (view.getId() == R.id.button_settings) {
            NavHostFragment.findNavController(fragmentTitle)
                    .navigate(actionFragmentTitleToFragmentSettings());
        } else if (view.getId() == R.id.button_folder_search) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String title = view.getResources().getString(R.string.pick_folder);
            Intent chooser = Intent.createChooser(intent, title);
            fragmentTitle.startActivityForResult(chooser, FragmentTitle.REQUEST_CODE_OPEN_FOLDER);
        }

    }

}
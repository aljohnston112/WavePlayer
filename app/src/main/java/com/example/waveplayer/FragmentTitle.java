package com.example.waveplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
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
import java.util.List;

import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewAdapter;

import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentFiles;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentPlaylists;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSettings;
import static com.example.waveplayer.FragmentTitleDirections.actionFragmentTitleToFragmentSongs;

public class FragmentTitle extends Fragment {

    ActivityMain activityMain;

    private static final int REQUEST_CODE_OPEN_FOLDER = 9367;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_title, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activityMain = ((ActivityMain) getActivity());
        updateMainContent();
        setUpButtons(view);
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
                List<TreeNode> nodes = new ArrayList<>();
                TreeNode<LayoutItemTypeDirectory> treeNode = new TreeNode<>(new LayoutItemTypeDirectory(uri.getLastPathSegment()));
                traverseDirectoryEntries(treeNode, uri);
                nodes.add(treeNode);
                activityMain.treeViewAdapter = new TreeViewAdapter(nodes,
                        Arrays.asList(new TreeViewBinderFile(), new TreeViewBinderDirectory()));
                activityMain.treeViewAdapter.setOnTreeNodeListener(new TreeViewAdapter.OnTreeNodeListener() {
                    @Override
                    public boolean onClick(TreeNode node, RecyclerView.ViewHolder holder) {
                        if (!node.isLeaf()) {
                            //Update and toggle the node.
                            onToggle(!node.isExpand(), holder);
                        }
                        return false;
                    }

                    @Override
                    public void onToggle(boolean isExpand, RecyclerView.ViewHolder holder) {
                        TreeViewBinderDirectory.ViewHolder
                                dirViewHolder = (TreeViewBinderDirectory.ViewHolder) holder;
                        final ImageView ivArrow = dirViewHolder.getIvArrow();
                        int rotateDegree = isExpand ? 90 : -90;
                        ivArrow.animate().rotationBy(rotateDegree).start();
                    }
                });

            }
        }
        NavHostFragment.findNavController(FragmentTitle.this)
                .navigate(actionFragmentTitleToFragmentFiles());
    }

    void traverseDirectoryEntries(TreeNode<LayoutItemTypeDirectory> app, Uri rootUri) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                rootUri, DocumentsContract.getTreeDocumentId(rootUri));

        getFiles(app, childrenUri, rootUri);
    }

    private void getFiles(TreeNode<LayoutItemTypeDirectory> treeNode, Uri childrenUri, Uri rootUri) {
        ContentResolver contentResolver = activityMain.getContentResolver();
            try (Cursor cursor = contentResolver.query(childrenUri, new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE},
                    null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(0);
                        String name = cursor.getString(1);
                        String mime = cursor.getString(2);
                        if (isDirectory(mime)) {
                            TreeNode treeNodeChild = new TreeNode(new LayoutItemTypeDirectory(name));
                            Uri newNode = DocumentsContract.buildChildDocumentsUriUsingTree(
                                    rootUri, docId);
                            getFiles(treeNodeChild, newNode, rootUri);
                            treeNode.addChild(treeNodeChild);
                        } else {
                            TreeNode treeNodeChild = new TreeNode(new LayoutItemTypeFile(name));
                            treeNode.addChild(treeNodeChild);
                        }
                    }
                }
            }
        }


    // Util method to check if the mime type is a directory
    private static boolean isDirectory(String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activityMain = null;
    }

}
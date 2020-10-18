package com.example.waveplayer;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewBinder;

public class TreeViewBinderFile extends TreeViewBinder<TreeViewBinderFile.ViewHolder> {

    @Override
    public TreeViewBinderFile.ViewHolder provideViewHolder(View itemView) {
        return new TreeViewBinderFile.ViewHolder(itemView);
    }

    @Override
    public void bindView(TreeViewBinderFile.ViewHolder viewHolder, int i, TreeNode treeNode) {
        LayoutItemTypeFile fileNode = (LayoutItemTypeFile) treeNode.getContent();
        viewHolder.tvName.setText(fileNode.fileName);
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_file;
    }

    public class ViewHolder extends TreeViewBinder.ViewHolder {

        public TextView tvName;

        public ImageView file;

        public ViewHolder(View rootView) {
            super(rootView);
            this.tvName = rootView.findViewById(R.id.text_view_file_name);
            this.file = rootView.findViewById(R.id.handle_file);
        }

    }

}
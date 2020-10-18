package com.example.waveplayer;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import tellh.com.recyclertreeview_lib.TreeNode;
import tellh.com.recyclertreeview_lib.TreeViewBinder;

public class TreeViewBinderDirectory extends TreeViewBinder<TreeViewBinderDirectory.ViewHolder> {

    @Override
    public ViewHolder provideViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void bindView(ViewHolder viewHolder, int i, TreeNode treeNode) {
        LayoutItemTypeDirectory fileNode = (LayoutItemTypeDirectory) treeNode.getContent();
        viewHolder.tvName.setText(fileNode.dirName);
    }


    @Override
    public int getLayoutId() {
        return R.layout.item_directory;
    }

    public class ViewHolder extends TreeViewBinder.ViewHolder {

        public TextView tvName;

        public ImageView arrow;

        public ViewHolder(View rootView) {
            super(rootView);
            this.tvName = rootView.findViewById(R.id.text_view_directory_name);
            this.arrow = rootView.findViewById(R.id.handle_directory);
        }

        public ImageView getIvArrow() {
            return arrow;
        }

    }

}
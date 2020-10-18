package com.example.waveplayer;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import recyclertreeview_lib.TreeViewHolder;
import recyclertreeview_lib.TreeViewNode;


public class TreeViewHolderDirectory extends TreeViewHolder<TreeViewHolderDirectory.ViewHolder> {

    @Override
    public ViewHolder getViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void bindView(ViewHolder viewHolder, int i, TreeViewNode treeNode) {
        LayoutItemTypeDirectory fileNode = (LayoutItemTypeDirectory) treeNode.getItem();
        viewHolder.tvName.setText(fileNode.dirName);
    }

    @Override
    public int getItemViewType() {
        return R.layout.item_directory;
    }

    public class ViewHolder extends TreeViewHolder.ViewHolder {

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
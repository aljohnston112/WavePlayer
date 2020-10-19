package recyclertreeview;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.R;

public class TreeViewHolderFile extends TreeViewHolder<TreeViewHolderFile.ViewHolder> {

    @Override
    public TreeViewHolderFile.ViewHolder getViewHolder(View itemView) {
        return new TreeViewHolderFile.ViewHolder(itemView);
    }

    @Override
    public void bindView(TreeViewHolderFile.ViewHolder viewHolder, int i, TreeViewNode treeNode) {
        LayoutItemTypeFile fileNode = (LayoutItemTypeFile) treeNode.getItem();
        viewHolder.tvName.setText(fileNode.fileName);
    }

    @Override
    public int getItemViewType() {
        return R.layout.item_file;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView tvName;

        public ImageView file;

        public ViewHolder(View rootView) {
            super(rootView);
            this.tvName = rootView.findViewById(R.id.text_view_file_name);
            this.file = rootView.findViewById(R.id.handle_file);
        }

    }

}
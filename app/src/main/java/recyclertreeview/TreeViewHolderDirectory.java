package recyclertreeview;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.waveplayer.R;


public class TreeViewHolderDirectory extends TreeViewHolder<TreeViewHolderDirectory.ViewHolder> {

    boolean isExpanded = false;

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean isExpanded){
        this.isExpanded = isExpanded;
    }

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

    public class ViewHolder extends RecyclerView.ViewHolder {

        boolean isExpanded = false;

        public boolean isExpanded() {
            return isExpanded;
        }

        public void setExpanded(boolean isExpanded){
            this.isExpanded = isExpanded;
        }

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
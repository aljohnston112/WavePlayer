package recyclertreeview;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public abstract class TreeViewHolder<VH extends RecyclerView.ViewHolder> implements LayoutItemType {

    public abstract VH getViewHolder(View itemView);

    public abstract <T extends LayoutItemType> void bindView(
            VH holder, int position, TreeViewNode<T> node);

}
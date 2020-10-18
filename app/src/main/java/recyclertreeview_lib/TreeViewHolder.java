package recyclertreeview_lib;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;

public abstract class TreeViewHolder<VH extends RecyclerView.ViewHolder> implements LayoutItemType {

    public abstract VH getViewHolder(View itemView);

    public abstract <T extends LayoutItemType> void bindView(
            VH holder, int position, TreeViewNode<T> node);

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View rootView) {
            super(rootView);
        }

        protected <T extends View> T findViewById(@IdRes int id) {
            return (T) itemView.findViewById(id);
        }

    }

}
package recyclertreeview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by tlh on 2016/10/1 :)
 * Edited by Alexander Johnston on 2020/10/18
 */
public class TreeViewAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<? extends TreeViewHolder<VH>> treeViewHolders;
    private final List<TreeViewNode> treeViewNodes;
    private OnTreeNodeListener onTreeNodeListener;

    private int padding = 30;

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public TreeViewAdapter(List<TreeViewNode> nodes,
                           List<? extends TreeViewHolder<VH>> treeViewHolders) {
        treeViewNodes = new ArrayList<>();
        if (nodes != null) {
            findDisplayNodes(nodes);
        }
        this.treeViewHolders = treeViewHolders;
    }

    private void findDisplayNodes(List<TreeViewNode> nodes) {
        for (TreeViewNode node : nodes) {
            treeViewNodes.add(node);
            if (!node.isLeaf() && node.expanded())
                findDisplayNodes(node.getChildList());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return treeViewNodes.get(position).getItem().getItemViewType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        if (treeViewHolders.size() == 1) {
            return treeViewHolders.get(0).getViewHolder(view);
        }
        for (TreeViewHolder<VH> viewBinder : treeViewHolders) {
            if (viewBinder.getItemViewType() == viewType)
                return viewBinder.getViewHolder(view);
        }
        throw new IllegalArgumentException("viewType passed to onCreateViewHolder was not found");
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        holder.itemView.setPaddingRelative(
                treeViewNodes.get(position).getTreeHeight() * padding,
                3, 3, 3);
        if (holder instanceof TreeViewHolderDirectory.ViewHolder && onTreeNodeListener != null) {
            boolean dirIsExpanded = treeViewNodes.get(position).isExpanded();
            if(dirIsExpanded && !((TreeViewHolderDirectory.ViewHolder) holder).isExpanded()) {
                onTreeNodeListener.toggle(true, holder);
                ((TreeViewHolderDirectory.ViewHolder) holder).setExpanded(true);
            } else if(!dirIsExpanded && ((TreeViewHolderDirectory.ViewHolder) holder).isExpanded()){
                onTreeNodeListener.toggle(false, holder);
                ((TreeViewHolderDirectory.ViewHolder) holder).setExpanded(false);
            }
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TreeViewNode clickedNode = treeViewNodes.get(holder.getLayoutPosition());
                if (onTreeNodeListener != null && onTreeNodeListener.onClick(clickedNode, holder))
                    return;
                if (clickedNode.isLeaf())
                    return;
                boolean expanded = clickedNode.expanded();
                int positionStart = treeViewNodes.indexOf(clickedNode) + 1;
                if (!expanded) {
                    notifyItemRangeInserted(positionStart, addChildNodes(clickedNode, positionStart));
                } else {
                    notifyItemRangeRemoved(
                            positionStart, removeChildNodes(clickedNode, true));
                }
            }
        });
        for (TreeViewHolder<VH> treeViewHolder : treeViewHolders) {
            if (treeViewHolder.getItemViewType()
                    == treeViewNodes.get(position).getItem().getItemViewType())
                treeViewHolder.bindView((VH) holder, position, treeViewNodes.get(position));
        }
    }

    private int addChildNodes(TreeViewNode viewNode, int startIndex) {
        List<TreeViewNode> childList = viewNode.getChildList();
        int nChildren = 0;
        for (TreeViewNode treeViewNode : childList) {
            treeViewNodes.add(startIndex + nChildren++, treeViewNode);
            if (treeViewNode.expanded()) {
                nChildren += addChildNodes(treeViewNode, startIndex + nChildren);
            }
        }
        if (!viewNode.expanded())
            viewNode.toggle();
        return nChildren;
    }

    private int removeChildNodes(TreeViewNode treeViewNode, boolean shouldToggle) {
        if (treeViewNode.isLeaf())
            return 0;
        List<TreeViewNode> childList = treeViewNode.getChildList();
        int removeChildCount = childList.size();
        treeViewNodes.removeAll(childList);
        for (TreeViewNode childTreeNode : childList) {
            if (childTreeNode.expanded()) {
                removeChildCount += removeChildNodes(childTreeNode, false);
            }
        }
        if (shouldToggle)
            treeViewNode.toggle();
        return removeChildCount;
    }

    @Override
    public int getItemCount() {
        return treeViewNodes == null ? 0 : treeViewNodes.size();
    }

    public void setOnTreeNodeListener(OnTreeNodeListener onTreeNodeListener) {
        this.onTreeNodeListener = onTreeNodeListener;
    }

    public interface OnTreeNodeListener {

        boolean onClick(TreeViewNode node, RecyclerView.ViewHolder holder);

        void toggle(boolean isExpanded, RecyclerView.ViewHolder holder);
    }

}
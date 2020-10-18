package recyclertreeview_lib;

import android.os.Build;
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

    private static final String KEY_IS_EXPANDED = "IS_EXPANDED";

    private final List<? extends TreeViewHolder<VH>> treeViewHolders;
    private final List<TreeViewNode> treeViewNodes;
    private OnTreeNodeListener onTreeNodeListener;

    private int padding = 30;
    public void setPadding(int padding) {
        this.padding = padding;
    }

    private boolean shouldCollapseChild;

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
        //return treeViewHolders.get(0).getViewHolder(view);
        throw new IllegalArgumentException("viewType passed to onCreateViewHolder was not found");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        if (payloads != null && !payloads.isEmpty()) {
            Bundle b = (Bundle) payloads.get(0);
            for (String key : b.keySet()) {
                switch (key) {
                    case KEY_IS_EXPANDED:
                        if (onTreeNodeListener != null)
                            onTreeNodeListener.toggle(b.getBoolean(key), holder);
                        break;
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            holder.itemView.setPaddingRelative(
                    treeViewNodes.get(position).getTreeHeight() * padding,
                    3, 3, 3);
        }else {
            holder.itemView.setPadding(
                    treeViewNodes.get(position).getTreeHeight() * padding,
                    3, 3, 3);
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

    private int removeChildNodes(TreeViewNode treeViewNode) {
        return removeChildNodes(treeViewNode, true);
    }

    private int removeChildNodes(TreeViewNode treeViewNode, boolean shouldToggle) {
        if (treeViewNode.isLeaf())
            return 0;
        List<TreeViewNode> childList = treeViewNode.getChildList();
        int removeChildCount = childList.size();
        treeViewNodes.removeAll(childList);
        for (TreeViewNode childTreeNode : childList) {
            if (childTreeNode.expanded()) {
                if (shouldCollapseChild)
                    childTreeNode.toggle();
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

    public void ifShouldCollapseChildWhileCollapsingParent(boolean toCollapseChild) {
        this.shouldCollapseChild = toCollapseChild;
    }

    public void setOnTreeNodeListener(OnTreeNodeListener onTreeNodeListener) {
        this.onTreeNodeListener = onTreeNodeListener;
    }

    public interface OnTreeNodeListener {

        boolean onClick(TreeViewNode node, RecyclerView.ViewHolder holder);

        void toggle(boolean isExpanded, RecyclerView.ViewHolder holder);
    }

    public void refresh(List<TreeViewNode> treeViewNodes) {
        this.treeViewNodes.clear();
        findDisplayNodes(treeViewNodes);
        notifyDataSetChanged();
    }

    public Iterator<TreeViewNode> getDisplayNodesIterator() {
        return treeViewNodes.iterator();
    }

    private void notifyDiff(final List<TreeViewNode> oldList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return treeViewNodes.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return recyclertreeview_lib.TreeViewAdapter.this.areItemsTheSame(
                        oldList.get(oldItemPosition), treeViewNodes.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return recyclertreeview_lib.TreeViewAdapter.this.areContentsTheSame(
                        oldList.get(oldItemPosition), treeViewNodes.get(newItemPosition));
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                return recyclertreeview_lib.TreeViewAdapter.this.getChangePayload(
                        oldList.get(oldItemPosition), treeViewNodes.get(newItemPosition));
            }

        });
        diffResult.dispatchUpdatesTo(this);
    }

    private Object getChangePayload(TreeViewNode oldNode, TreeViewNode newNode) {
        Bundle diffBundle = new Bundle();
        if (newNode.expanded() != oldNode.expanded()) {
            diffBundle.putBoolean(KEY_IS_EXPANDED, newNode.expanded());
        }
        if (diffBundle.size() == 0)
            return null;
        return diffBundle;
    }

    private boolean areContentsTheSame(TreeViewNode oldNode, TreeViewNode newNode) {
        return oldNode.getItem() != null && oldNode.getItem().equals(newNode.getItem())
                && oldNode.expanded() == newNode.expanded();
    }

    private boolean areItemsTheSame(TreeViewNode oldNode, TreeViewNode newNode) {
        return oldNode.getItem() != null && oldNode.getItem().equals(newNode.getItem());
    }

    public void collapseRoots() {
        List<TreeViewNode> oldTreeViewNodes = backupDisplayNodes();
        List<TreeViewNode> rootTreeViews = new ArrayList<>();
        for (TreeViewNode treeViewNode : treeViewNodes) {
            if (treeViewNode.isRoot())
                rootTreeViews.add(treeViewNode);
        }
        for (TreeViewNode rootTreeView : rootTreeViews) {
            if (rootTreeView.expanded())
                removeChildNodes(rootTreeView);
        }
        notifyDiff(oldTreeViewNodes);
    }

    @NonNull
    private List<TreeViewNode> backupDisplayNodes() {
        List<TreeViewNode> temp = new ArrayList<>();
        for (TreeViewNode displayNode : treeViewNodes) {
                temp.add(displayNode.clone());
        }
        return temp;
    }

    public void collapseNode(TreeViewNode treeViewNode) {
        List<TreeViewNode> temp = backupDisplayNodes();
        removeChildNodes(treeViewNode);
        notifyDiff(temp);
    }

    public void collapseBrotherNode(TreeViewNode treeViewNode) {
        List<TreeViewNode> temp = backupDisplayNodes();
        if (treeViewNode.isRoot()) {
            List<TreeViewNode> roots = new ArrayList<>();
            for (TreeViewNode viewNode : treeViewNodes) {
                if (viewNode.isRoot())
                    roots.add(viewNode);
            }
            //Close all root nodes.
            for (TreeViewNode root : roots) {
                if (root.expanded() && !root.equals(treeViewNode))
                    removeChildNodes(root);
            }
        } else {
            TreeViewNode parent = treeViewNode.getParent();
            if (parent == null)
                return;
            List<TreeViewNode> childList = parent.getChildList();
            for (TreeViewNode node : childList) {
                if (node.equals(treeViewNode) || !node.expanded())
                    continue;
                removeChildNodes(node);
            }
        }
        notifyDiff(temp);
    }

}
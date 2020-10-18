package recyclertreeview_lib;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tlh on 2016/10/1 :)
 * Edited by Alexander Johnston on 2020/10/18
 */
public class TreeViewNode<T extends LayoutItemType> implements Cloneable {

    private static final int UNDEFINED = -1;

    private TreeViewNode<T> parent;
    public TreeViewNode<T> getParent() {
        return parent;
    }

    private List<TreeViewNode> childList;

    private final T item;
    public T getItem() {
        return item;
    }

    private boolean isExpanded;
    private int treeHeight = UNDEFINED;

    public TreeViewNode(@NonNull T item) {
        this.item = item;
        this.childList = new ArrayList<>();
    }

    public int getTreeHeight() {
        if (isRoot())
            treeHeight = 0;
        else if (treeHeight == UNDEFINED) {
            treeHeight = parent.getTreeHeight() + 1;
        }
        return treeHeight;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return childList == null || childList.isEmpty();
    }

    public List<TreeViewNode> getChildList() {
        return childList;
    }

    public TreeViewNode addChild(TreeViewNode node) {
        if (childList == null) {
            childList = new ArrayList<>();
        }
        childList.add(node);
        node.parent = this;
        return this;
    }

    public boolean toggle() {
        isExpanded = !isExpanded;
        return isExpanded;
    }

    public void collapse() {
        if (isExpanded) {
            isExpanded = false;
        }
    }

    public void collapseAll() {
        collapse();
        if (childList == null || childList.isEmpty()) {
            return;
        }
        for (TreeViewNode<T> child : this.childList) {
            child.collapseAll();
        }
    }

    public void expand() {
        if (!isExpanded) {
            isExpanded = true;
        }
    }

    public void expandAll() {
        expand();
        if (childList == null || childList.isEmpty()) {
            return;
        }
        for (TreeViewNode<T> child : this.childList) {
            child.expandAll();
        }
    }

    public boolean expanded() {
        return isExpanded;
    }

    @Override
    public String toString() {
        return "TreeNode<T>{" +
                "item=" + this.item +
                ", parent=" + (parent == null ? "null" : parent.getItem().toString()) +
                ", childList=" + (childList == null ? "null" : childList.toString()) +
                ", isExpanded=" + isExpanded +
                '}';
    }

    @Override
    protected TreeViewNode<T> clone() {
        TreeViewNode<T> clone = new TreeViewNode<>(this.item);
        clone.isExpanded = this.isExpanded;
        return clone;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

}

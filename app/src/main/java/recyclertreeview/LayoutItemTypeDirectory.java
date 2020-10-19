package recyclertreeview;


import com.example.waveplayer.R;

import recyclertreeview.LayoutItemType;

public class LayoutItemTypeDirectory implements LayoutItemType {

    public String dirName;

    public LayoutItemTypeDirectory(String dirName) {
        this.dirName = dirName;
    }

    @Override
    public int getItemViewType() {
        return R.layout.item_directory;
    }

}
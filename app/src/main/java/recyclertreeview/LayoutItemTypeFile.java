package recyclertreeview;

import com.example.waveplayer.R;

import recyclertreeview.LayoutItemType;

public class LayoutItemTypeFile implements LayoutItemType {

    public String fileName;

    public LayoutItemTypeFile(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public int getItemViewType() {
        return R.layout.item_file;
    }

}

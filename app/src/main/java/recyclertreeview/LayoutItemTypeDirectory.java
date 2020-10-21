package recyclertreeview;


import android.net.Uri;

import com.example.waveplayer.R;

import recyclertreeview.LayoutItemType;

public class LayoutItemTypeDirectory implements LayoutItemType {

    public Uri uri;

    public String dirName;

    public LayoutItemTypeDirectory(Uri uri, String dirName) {
        this.dirName = dirName;
        this.uri = uri;
    }

    @Override
    public int getItemViewType() {
        return R.layout.item_directory;
    }

}
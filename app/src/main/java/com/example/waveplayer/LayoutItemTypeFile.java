package com.example.waveplayer;

import recyclertreeview_lib.LayoutItemType;

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

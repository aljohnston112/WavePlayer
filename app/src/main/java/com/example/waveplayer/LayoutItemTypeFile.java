package com.example.waveplayer;

import tellh.com.recyclertreeview_lib.LayoutItemType;

public class LayoutItemTypeFile implements LayoutItemType {

    public String fileName;

    public LayoutItemTypeFile(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_file;
    }

}

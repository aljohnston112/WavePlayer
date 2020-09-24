package com.example.waveplayer;

import android.graphics.Bitmap;
import android.net.Uri;


import androidx.annotation.Size;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.URI;

public final class WaveURI {

    public final Uri uri;

    public final String name;

    final Bitmap thumbnail;

    public WaveURI(Uri uri, Bitmap thumbnail, String name){
        this.uri = uri;
        this.name = name;
        this.thumbnail = thumbnail;

    }

}

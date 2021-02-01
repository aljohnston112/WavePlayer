package com.example.waveplayer.media_controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Size;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BitmapLoader {

    public static Bitmap getThumbnail(Uri uri, int width, int height, Context context) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                bitmap = context.getContentResolver().loadThumbnail(
                        uri, new Size(width, height), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(context.getContentResolver().openFileDescriptor(
                        uri, "r").getFileDescriptor());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            InputStream inputStream = null;
            if (mmr.getEmbeddedPicture() != null) {
                inputStream = new ByteArrayInputStream(mmr.getEmbeddedPicture());
            }
            mmr.release();
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                return getResizedBitmap(bitmap, width, height);
            }
        }
        return bitmap;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
    }

}

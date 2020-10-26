package com.example.waveplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import static com.example.waveplayer.FragmentPaneSong.getResizedBitmap;

public class OnLayoutChangeListenerSongPane implements View.OnLayoutChangeListener {

    FragmentPaneSong fragmentPaneSong;

    OnLayoutChangeListenerSongPane(FragmentPaneSong fragmentPaneSong){
        this.fragmentPaneSong = fragmentPaneSong;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        setUpSongArt(v);
        setUpPrev(v);
        setUpPlay(v);
        setUpNext(v);
    }

    private void setUpNext(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonSongPaneNext);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable = ResourcesCompat.getDrawable(
                fragmentPaneSong.getResources(), R.drawable.skip_next_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpPlay(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonSongPanePlayPause);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable;
        ActivityMain activityMain = ((ActivityMain) fragmentPaneSong.getActivity());
        if (activityMain != null && activityMain.serviceMain != null && activityMain.serviceMain.isPlaying()) {
            drawable = ResourcesCompat.getDrawable(
                    fragmentPaneSong.getResources(), R.drawable.pause_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(
                    fragmentPaneSong.getResources(), R.drawable.play_arrow_black_24dp, null);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpPrev(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonSongPanePrev);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable = ResourcesCompat.getDrawable(
                fragmentPaneSong.getResources(), R.drawable.skip_previous_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpSongArt(View view) {
        ImageView imageViewSongArt = view.findViewById(R.id.imageViewSongPaneSongArt);
        int height = imageViewSongArt.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawableSongArt = ResourcesCompat.getDrawable(
                fragmentPaneSong.getResources(), R.drawable.music_note_black_48dp, null);
        if (drawableSongArt != null) {
            drawableSongArt.setBounds(0, 0, width, height);
            Bitmap bitmapSongArt = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapSongArt);
            drawableSongArt.draw(canvas);
            Bitmap bitmapSongArtResized = getResizedBitmap(bitmapSongArt, width, height);
            bitmapSongArt.recycle();
            imageViewSongArt.setImageBitmap(bitmapSongArtResized);
        }
    }

}

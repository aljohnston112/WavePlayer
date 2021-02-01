package com.example.waveplayer.fragments.fragment_song;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import com.example.waveplayer.activity_main.ActivityMain;
import com.example.waveplayer.R;
import com.example.waveplayer.media_controller.BitmapLoader;
import com.example.waveplayer.media_controller.MediaData;

public class OnLayoutChangeListenerFragmentSongButtons implements View.OnLayoutChangeListener {

    private final ActivityMain activityMain;

    OnLayoutChangeListenerFragmentSongButtons(ActivityMain activityMain){
        this.activityMain = activityMain;
    }

    @Override
    public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if(activityMain.fragmentSongVisible()) {
            setUpGood(view);
            setUpBad(view);
            setUpShuffle(view);
            setUpPrev(view);
            setUpPlay(view);
            setUpNext(view);
            setUpLoop(view);
        }
    }

    private void setUpGood(View view) {
        ImageView imageView = view.findViewById(R.id.button_thumb_up);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(),
                R.drawable.thumb_up_alt_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpBad(View view) {
        ImageView imageView = view.findViewById(R.id.button_thumb_down);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(),
                R.drawable.thumb_down_alt_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpShuffle(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonShuffle);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable;
        if (activityMain.shuffling()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.ic_shuffle_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.ic_shuffle_white_24dp, null);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpNext(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonNext);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.skip_next_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpPlay(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonPlayPause);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable;
        if (activityMain != null && activityMain.isPlaying()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.pause_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.play_arrow_black_24dp, null);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpPrev(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonPrev);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable = ResourcesCompat.getDrawable(view.getResources(), R.drawable.skip_previous_black_24dp, null);
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

    private void setUpLoop(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonRepeat);
        int width = imageView.getMeasuredWidth();
        //noinspection SuspiciousNameCombination
        int height = width;
        Drawable drawable;

        if (activityMain.loopingOne()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.repeat_one_black_24dp, null);
        } else if (activityMain.looping()) {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.repeat_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(view.getResources(),
                    R.drawable.repeat_white_24dp, null);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            Bitmap bitmapResized = BitmapLoader.getResizedBitmap(bitmap, width, height);
            bitmap.recycle();
            imageView.setImageBitmap(bitmapResized);
        }
    }

}
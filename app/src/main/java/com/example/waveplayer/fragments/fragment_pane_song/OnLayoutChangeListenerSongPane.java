package com.example.waveplayer.fragments.fragment_pane_song;

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

public class OnLayoutChangeListenerSongPane implements View.OnLayoutChangeListener {

    private final FragmentPaneSong fragmentPaneSong;

    OnLayoutChangeListenerSongPane(FragmentPaneSong fragmentPaneSong){
        this.fragmentPaneSong = fragmentPaneSong;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        ActivityMain activityMain = ((ActivityMain)fragmentPaneSong.getActivity());
        if(activityMain.findViewById(R.id.fragmentSongPane).getVisibility() == View.VISIBLE &&
                !((ActivityMain)fragmentPaneSong.getActivity()).fragmentSongVisible()) {
            fragmentPaneSong.updateSongPaneUI();
            setUpPrev(v);
            setUpPlay(v);
            setUpNext(v);

        }
    }

    private void setUpNext(View view) {
        ImageView imageView = view.findViewById(R.id.imageButtonSongPaneNext);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable = ResourcesCompat.getDrawable(
                fragmentPaneSong.getResources(), R.drawable.skip_next_black_24dp, null);
        if (drawable != null && width > 0) {
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
        ImageView imageView = view.findViewById(R.id.imageButtonSongPanePlayPause);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable;
        ActivityMain activityMain = ((ActivityMain) fragmentPaneSong.getActivity());
        if (activityMain != null && activityMain.serviceConnected() && activityMain.isPlaying()) {
            drawable = ResourcesCompat.getDrawable(
                    fragmentPaneSong.getResources(), R.drawable.pause_black_24dp, null);
        } else {
            drawable = ResourcesCompat.getDrawable(
                    fragmentPaneSong.getResources(), R.drawable.play_arrow_black_24dp, null);
        }
        if (drawable != null && width > 0) {
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
        ImageView imageView = view.findViewById(R.id.imageButtonSongPanePrev);
        int height = imageView.getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        int width = height;
        Drawable drawable = ResourcesCompat.getDrawable(
                fragmentPaneSong.getResources(), R.drawable.skip_previous_black_24dp, null);
        if (drawable != null && width > 0) {
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
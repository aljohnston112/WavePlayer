package com.example.waveplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class FragmentSongPane extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_song_pane, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSongPane(view);
    }

    private void setupSongPane(final View view) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                setUpSongArt();
                setUpPrev();
                setUpPlay();
                setUpNext();
            }

            private void setUpNext() {
                ImageView imageView = view.findViewById(R.id.imageButtonSongPaneNext);
                int height = imageView.getMeasuredHeight();
                //noinspection SuspiciousNameCombination
                int width = height;
                Drawable drawable = getResources().getDrawable(R.drawable.skip_next_black_24dp);
                drawable.setBounds(0, 0, width, height);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.draw(canvas);
                Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
                bitmap.recycle();
                imageView.setImageBitmap(bitmapResized);
            }

            private void setUpPlay() {
                ImageView imageView = view.findViewById(R.id.imageButtonSongPanePlayPause);
                int height = imageView.getMeasuredHeight();
                //noinspection SuspiciousNameCombination
                int width = height;
                Drawable drawable;
                ActivityMain activityMain = ((ActivityMain)getActivity());
                if(activityMain != null && activityMain.isPlaying()) {
                    drawable = getResources().getDrawable(R.drawable.pause_black_24dp);
                } else{
                    drawable = getResources().getDrawable(R.drawable.play_arrow_black_24dp);
                }
                drawable.setBounds(0, 0, width, height);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.draw(canvas);
                Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
                bitmap.recycle();
                imageView.setImageBitmap(bitmapResized);
            }

            private void setUpPrev() {
                ImageView imageView = view.findViewById(R.id.imageButtonSongPanePrev);
                int height = imageView.getMeasuredHeight();
                //noinspection SuspiciousNameCombination
                int width = height;
                Drawable drawable = getResources().getDrawable(R.drawable.skip_previous_black_24dp);
                drawable.setBounds(0, 0, width, height);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.draw(canvas);
                Bitmap bitmapResized = getResizedBitmap(bitmap, width, height);
                bitmap.recycle();
                imageView.setImageBitmap(bitmapResized);
            }

            private void setUpSongArt() {
                ImageView imageViewSongArt = view.findViewById(R.id.imageViewSongPaneSongArt);
                int height = imageViewSongArt.getMeasuredHeight();
                //noinspection SuspiciousNameCombination
                int width = height;
                Drawable drawableSongArt = getResources().getDrawable(R.drawable.music_note_black_48dp);
                drawableSongArt.setBounds(0, 0, width, height);
                Bitmap bitmapSongArt = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmapSongArt);
                drawableSongArt.draw(canvas);
                Bitmap bitmapSongArtResized = getResizedBitmap(bitmapSongArt, width, height);
                bitmapSongArt.recycle();
                imageViewSongArt.setImageBitmap(bitmapSongArtResized);
            }
        });
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
    }


}
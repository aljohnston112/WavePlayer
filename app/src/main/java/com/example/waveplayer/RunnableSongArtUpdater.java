package com.example.waveplayer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import static com.example.waveplayer.fragments.fragment_pane_song.FragmentPaneSong.getResizedBitmap;

public class RunnableSongArtUpdater implements Runnable {

    private final ActivityMain activityMain;

    public RunnableSongArtUpdater(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    public void run() {
        ImageView imageViewSongArt = activityMain.findViewById(R.id.image_view_song_art);
        if(imageViewSongArt != null) {
            int songArtHeight = imageViewSongArt.getHeight();
            int songArtWidth = imageViewSongArt.getWidth();
            if (songArtWidth > songArtHeight) {
                songArtWidth = songArtHeight;
            } else {
                songArtHeight = songArtWidth;
            }
            Bitmap bitmap = ActivityMain.getThumbnail(activityMain.getCurrentSong(),
                    songArtWidth, songArtHeight, activityMain.getApplicationContext());
            if (bitmap == null) {
                Drawable drawable = ResourcesCompat.getDrawable(imageViewSongArt.getResources(),
                        R.drawable.music_note_black_48dp, null);
                if (drawable != null) {
                    drawable.setBounds(0, 0, songArtWidth, songArtHeight);
                    Bitmap bitmapDrawable = Bitmap.createBitmap(songArtWidth, songArtHeight, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmapDrawable);
                    Paint paint = new Paint();
                    paint.setColor(imageViewSongArt.getResources().getColor(R.color.colorPrimary));
                    canvas.drawRect(0, 0, songArtWidth, songArtHeight, paint);
                    drawable.draw(canvas);
                    Bitmap bitmapResized = getResizedBitmap(bitmapDrawable, songArtWidth, songArtHeight);
                    bitmapDrawable.recycle();
                    imageViewSongArt.setImageBitmap(bitmapResized);
                }
            } else {
                imageViewSongArt.setImageBitmap(bitmap);
            }
        }
    }

}
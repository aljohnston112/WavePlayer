package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverNotification extends BroadcastReceiver {

    ActivityMain activityMain;

    BroadcastReceiverNotification(ActivityMain activityMain){
        this.activityMain = activityMain;
    }

    public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && activityMain != null) {
                switch (action) {
                    case "Next":
                            activityMain.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activityMain.playNext();
                                    activityMain.updateSongPaneUI();
                                    activityMain.updateSongUI();
                                }
                            });
                        break;
                    case "PlayPause":
                        if(activityMain.serviceMain.currentSong != null) {
                            activityMain.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activityMain.pauseOrPlay();
                                    activityMain.updateSongPaneUI();
                                    activityMain.updateSongUI();
                                }
                            });
                        }
                        break;
                    case "Previous":
                        if(activityMain.serviceMain.currentSong != null) {
                            activityMain.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    activityMain.playPrevious();
                                    activityMain.updateSongPaneUI();
                                    activityMain.updateSongUI();
                                }
                            });
                        }
                }
            }
    }

}

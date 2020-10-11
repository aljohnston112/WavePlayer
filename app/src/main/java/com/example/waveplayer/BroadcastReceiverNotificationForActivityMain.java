package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverNotificationForActivityMain extends BroadcastReceiver {

    ActivityMain activityMain;

    BroadcastReceiverNotificationForActivityMain(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && activityMain != null) {
            switch (action) {
                case "Next":
                case "PlayPause":
                case "Previous":
                    activityMain.updateSongPaneUI();
                    activityMain.updateSongUI();
                    break;
            }
        }
    }

}

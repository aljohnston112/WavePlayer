package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiverNotificationForActivityMainMediaControls extends BroadcastReceiver {

    ActivityMain activityMain;

    BroadcastReceiverNotificationForActivityMainMediaControls(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    public void onReceive(Context context, Intent intent) {
        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationForActivityMainMediaControls start");
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
        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationForActivityMainMediaControls end");
    }

}

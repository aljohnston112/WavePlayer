package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverOnCompletion extends BroadcastReceiver {

    ActivityMain activityMain;

    public BroadcastReceiverOnCompletion(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equals("Complete")) {
            if (activityMain != null) {
                activityMain.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activityMain.updateSongUI();
                        activityMain.updateSongPaneUI();
                    }
                });
            }
        }
    }

}

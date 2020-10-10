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
        if (action.equals(activityMain.getResources().getString(R.string.broadcast_receiver_action_on_completion))) {
            if (activityMain != null) {
                activityMain.updateSongUI();
                activityMain.updateSongPaneUI();
                if (activityMain.serviceMain != null) {
                    activityMain.serviceMain.updateNotification();
                }
            }
        }
    }

}

package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiverNotificationButtonsForActivityMain extends BroadcastReceiver {

    ActivityMain activityMain;

    BroadcastReceiverNotificationButtonsForActivityMain(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    public void onReceive(Context context, Intent intent) {
        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationButtonsForActivityMain start");
        String action = intent.getAction();
        if (action != null && activityMain != null) {
            if (action.equals(activityMain.getResources().getString(
                    R.string.broadcast_receiver_action_next)) ||
                    action.equals(activityMain.getResources().getString(
                            R.string.broadcast_receiver_action_play_pause)) ||
                    action.equals(activityMain.getResources().getString(
                            R.string.broadcast_receiver_action_previous))) {
                activityMain.updateUI();
            }
        }
        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationButtonsForActivityMain end");
    }

}

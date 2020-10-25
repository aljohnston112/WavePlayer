package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiverOnCompletion extends BroadcastReceiver {

    ActivityMain activityMain;

    public BroadcastReceiverOnCompletion(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(ActivityMain.TAG, "BroadcastReceiverOnCompletion start");
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(activityMain.getResources().getString(R.string.broadcast_receiver_action_on_completion))) {
                if (activityMain != null) {
                    activityMain.updateUI();
                }
            }
        }
        Log.v(ActivityMain.TAG, "BroadcastReceiverOnCompletion end");
    }

}

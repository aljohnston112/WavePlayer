package com.example.waveplayer.activity_main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.waveplayer.R;

public class BroadcastReceiverNotificationButtonsForActivityMain extends BroadcastReceiver {

    private final ActivityMain activityMain;

    BroadcastReceiverNotificationButtonsForActivityMain(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationButtonsForActivityMain start");

        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationButtonsForActivityMain end");
    }

}
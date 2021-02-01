package com.example.waveplayer.activity_main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.waveplayer.R;
import com.example.waveplayer.activity_main.ActivityMain;

public class BroadcastReceiverOnCompletion extends BroadcastReceiver {

    private final ActivityMain activityMain;

    public BroadcastReceiverOnCompletion(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(ActivityMain.TAG, "BroadcastReceiverOnCompletion start");
        String action = intent.getAction();
        if (action != null) {

        }
        Log.v(ActivityMain.TAG, "BroadcastReceiverOnCompletion end");
    }



}

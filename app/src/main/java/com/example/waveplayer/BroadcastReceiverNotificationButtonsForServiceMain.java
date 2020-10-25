package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiverNotificationButtonsForServiceMain extends BroadcastReceiver {

    ServiceMain serviceMain;

    BroadcastReceiverNotificationButtonsForServiceMain(ServiceMain serviceMain) {
        this.serviceMain = serviceMain;
    }

    public void onReceive(Context context, Intent intent) {
        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationForServiceMainMediaControls start");
        String action = intent.getAction();
        if (action != null && serviceMain != null) {
            switch (action) {
                case "Next":
                    serviceMain.playNext();
                    break;
                case "PlayPause":
                    serviceMain.pauseOrPlay();
                    break;
                case "Previous":
                    serviceMain.playPrevious();
            }
        }
        Log.v(ActivityMain.TAG, "BroadcastReceiverNotificationForServiceMainMediaControls end");
    }

}

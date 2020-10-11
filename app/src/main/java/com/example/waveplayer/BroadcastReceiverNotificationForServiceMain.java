package com.example.waveplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastReceiverNotificationForServiceMain extends BroadcastReceiver {

    ServiceMain serviceMain;

    BroadcastReceiverNotificationForServiceMain(ServiceMain serviceMain) {
        this.serviceMain = serviceMain;
    }

    public void onReceive(Context context, Intent intent) {
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
    }

}

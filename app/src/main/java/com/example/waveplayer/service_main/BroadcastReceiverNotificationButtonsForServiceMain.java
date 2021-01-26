package com.example.waveplayer.service_main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.waveplayer.media_controller.SaveFile;

public class BroadcastReceiverNotificationButtonsForServiceMain extends BroadcastReceiver {

    private final ServiceMain serviceMain;

    BroadcastReceiverNotificationButtonsForServiceMain(ServiceMain serviceMain) {
        this.serviceMain = serviceMain;
    }

    public void onReceive(Context context, Intent intent) {
        Log.v(ServiceMain.TAG, "BroadcastReceiverNotificationForServiceMainMediaControls start");
        synchronized (ServiceMain.lock) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "Next":
                        serviceMain.playNext();
                        SaveFile.saveFile(serviceMain.getApplicationContext());
                        break;
                    case "PlayPause":
                        serviceMain.pauseOrPlay();
                        break;
                    case "Previous":
                        serviceMain.playPrevious();
                }
            }
        }
        Log.v(ServiceMain.TAG, "BroadcastReceiverNotificationForServiceMainMediaControls end");
    }

}

package com.example.waveplayer.activity_main;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.example.waveplayer.R;
import com.example.waveplayer.service_main.ServiceMain;

public class ConnectionServiceMain implements ServiceConnection {

    private ActivityMain activityMain;

    ConnectionServiceMain(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.v(ActivityMain.TAG, "onServiceConnected started");
        ServiceMain.ServiceMainBinder binder = (ServiceMain.ServiceMainBinder) service;
        activityMain.setServiceMain(binder.getService());
        sendBroadcast();
        Log.v(ActivityMain.TAG, "onServiceConnected ended");
    }

    private void sendBroadcast() {
        Log.v(ActivityMain.TAG, "Sending Broadcast onServiceConnected");
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        activityMain.sendBroadcast(intent);
        Log.v(ActivityMain.TAG, "Done sending Broadcast onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        Log.v(ActivityMain.TAG, "onServiceDisconnected start");
        activityMain.serviceDisconnected();
        activityMain = null;
    }

}
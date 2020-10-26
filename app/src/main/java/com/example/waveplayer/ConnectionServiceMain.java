package com.example.waveplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class ConnectionServiceMain implements ServiceConnection {

    ActivityMain activityMain;

    ConnectionServiceMain(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.v(ActivityMain.TAG, "onServiceConnected started");
        ServiceMain.ServiceMainBinder binder = (ServiceMain.ServiceMainBinder) service;
        activityMain.serviceMain = binder.getService();
        activityMain.setUpBroadcastReceivers();
        activityMain.askForExternalStoragePermissionAndFetchMediaFiles();
        activityMain.setUpSongPane();
        if (activityMain.serviceMain.currentSong != null) {
            activityMain.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activityMain.updateUI();
                }
            });
        }
        sendBroadcast();
        Log.v(ActivityMain.TAG, "onServiceConnected ended");
    }

    private void sendBroadcast() {
        Log.v(ActivityMain.TAG, "Sending Broadcast onCompletion");
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(activityMain.getResources().getString(
                R.string.broadcast_receiver_action_service_connected));
        activityMain.sendBroadcast(intent);
        Log.v(ActivityMain.TAG, "Done sending Broadcast onCompletion");
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        Log.v(ActivityMain.TAG, "onServiceDisconnected start");
        activityMain.onDestinationChangedListenerPanes = null;
        activityMain.onClickListenerSongPane = null;

    }

}
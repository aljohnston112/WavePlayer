package com.example.waveplayer;

public class RunnableUIUpdate implements Runnable {

    private final ActivityMain activityMain;

    private RunnableUIUpdate(){
        throw new UnsupportedOperationException();
    }

    public RunnableUIUpdate(ActivityMain activityMain) {
        this.activityMain = activityMain;
    }

    @Override
    public void run() {
        activityMain.updateUI();
    }

}

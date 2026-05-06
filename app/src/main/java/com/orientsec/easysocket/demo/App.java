package com.orientsec.easysocket.demo;

import android.app.Application;

import com.orientsec.easysocket.EasySocket;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EasySocket.getInstance().initialize(this);
    }
}

package com.orientsec.easysocket.demo;

import android.app.Application;

import com.orientsec.easysocket.EasySocket;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket
 * Time: 2018/01/26 13:18
 * Author: Fredric
 * coding is art not science
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EasySocket.getInstance().initialize(this);
    }
}

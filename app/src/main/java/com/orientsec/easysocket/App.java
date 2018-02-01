package com.orientsec.easysocket;

import android.app.Application;

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
        EasySocket.init(this);
    }
}

package com.orientsec.easysocket.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.Options;
import com.orientsec.easysocket.SocketClient;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.utils
 * Time: 2018/01/09 10:04
 * Author: Fredric
 * coding is art not science
 */
class AndroidLogger implements Logger {
    private static final String TAG = "EasySocket";
    private SocketClient socketClient;
    private final Options options;

    public AndroidLogger(Options options) {
        this.options = options;
    }

    private String formatMsg(String msg) {
        if (options.isDetailLog()) {
            StringBuilder sb = new StringBuilder(msg);
            sb.append(" {Name:[");
            if (socketClient != null) {
                sb.append(options.getName());
            }

            sb.append("]. Address:[");
            SocketClient socketClient = this.socketClient;
            if (socketClient != null) {
                Address address = socketClient.getAddress();
                if (address != null) {
                    sb.append(address);
                }
            }
            sb.append("] Thread:[");
            sb.append(Thread.currentThread().getName());
            sb.append("]}");
            return sb.toString();
        } else {
            return msg;
        }
    }

    @Override
    public void e(String msg) {
        Log.e(TAG, formatMsg(msg));
    }

    @Override
    public void e(String msg, Throwable t) {
        Log.e(TAG, formatMsg(msg), t);
    }

    @Override
    public void i(String msg) {
        Log.i(TAG, formatMsg(msg));
    }

    @Override
    public void i(String msg, Throwable t) {
        Log.i(TAG, formatMsg(msg), t);
    }

    @Override
    public void w(String msg) {
        Log.w(TAG, formatMsg(msg));
    }

    @Override
    public void w(String msg, Throwable t) {
        Log.w(TAG, formatMsg(msg), t);
    }

    @Override
    public void d(String msg) {
        Log.d(TAG, formatMsg(msg));
    }

    @Override
    public void attach(@NonNull SocketClient socketClient) {
        this.socketClient = socketClient;
    }
}

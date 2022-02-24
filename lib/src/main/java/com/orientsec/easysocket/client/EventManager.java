package com.orientsec.easysocket.client;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class EventManager implements Handler.Callback {
    private final Set<EventListener> listeners = new CopyOnWriteArraySet<>();
    final Handler mHandler;

    public EventManager(Looper looper) {
        mHandler = new Handler(looper, this);
    }

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void publish(int eventId) {
        mHandler.sendEmptyMessage(eventId);
    }

    public void publish(int eventId, long delay) {
        mHandler.sendEmptyMessageDelayed(eventId, delay);
    }

    public void publish(int eventId, Object object) {
        mHandler.obtainMessage(eventId, object).sendToTarget();
    }

    public void publish(int eventId, Object object, long delay) {
        Message message = mHandler.obtainMessage(eventId, object);
        mHandler.sendMessageDelayed(message, delay);
    }

    public void remove(int eventId) {
        mHandler.removeMessages(eventId);
    }

    public void remove(int eventId, Object object) {
        mHandler.removeMessages(eventId, object);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        for (EventListener listener : listeners) {
            listener.onEvent(msg.what, msg.obj);
        }
        return true;
    }
}

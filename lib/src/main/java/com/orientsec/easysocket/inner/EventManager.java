package com.orientsec.easysocket.inner;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.orientsec.easysocket.ConnectionManager;

import java.util.HashSet;
import java.util.Set;

public class EventManager implements Handler.Callback {
    private Set<EventListener> listeners = new HashSet<>();
    final Handler mHandler;

    public EventManager() {
        mHandler = new Handler(ConnectionManager.getInstance().getMainLooper(), this);
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

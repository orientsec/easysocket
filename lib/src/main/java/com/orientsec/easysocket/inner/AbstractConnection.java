package com.orientsec.easysocket.inner;


import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.ConnectEventListener;
import com.orientsec.easysocket.Connection;
import com.orientsec.easysocket.task.TaskManager;

public abstract class AbstractConnection implements Connection, EventListener,
        ConnectEventListener {

    protected abstract void onStart();

    protected abstract void onStop();

    protected abstract void onShutdown();

    public abstract void onNetworkAvailable();

    public abstract TaskManager getTaskManager();

    public abstract Address obtainAddress();
}

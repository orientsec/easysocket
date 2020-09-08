package com.orientsec.easysocket.inner;

public interface Events {
    int START = 1;
    int STOP = 2;
    int SHUTDOWN = 3;
    int RESTART = 9;

    int CONNECT_ERROR = 4;
    int CONNECT_SUCCESS = 5;
    int CONNECT_FAILED = 6;
    int AVAILABLE = 7;
    int INIT_FAILED = 8;
    int PULSE = 14;
    int ON_PACKET = 10;

    int NET_AVAILABLE = 11;

    int TASK_START = -1;
    int TASK_ENQUEUE = -2;
    int TASK_SEND = -3;
    int TASK_SUCCESS = -4;
    int TASK_ERROR = -5;
    int TASK_CANCEL = -6;
    int TASK_TIME_OUT = -7;
}

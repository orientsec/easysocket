package com.orientsec.easysocket.inner;

public interface Events {
    int START = 1;
    int SHUTDOWN = 2;
    int CONNECT_ERROR = 3;
    int CONNECT_SUCCESS = 4;
    int CONNECT_FAILED = 5;
    int AVAILABLE = 6;
    int INIT_FAILED = 7;

    int RESTART = 8;

    int ON_PACKET = 10;

    int NET_AVAILABLE = 11;

    int PULSE = 14;

    int TASK_START = -1;
    int TASK_ENQUEUE = -2;
    int TASK_SEND = -3;
    int TASK_SUCCESS = -4;
    int TASK_ERROR = -5;
    int TASK_CANCEL = -6;
    int TASK_TIME_OUT = -7;
}

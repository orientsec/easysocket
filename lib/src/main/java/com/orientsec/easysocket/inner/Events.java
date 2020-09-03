package com.orientsec.easysocket.inner;

public interface Events {
    int START = 1;
    int STOP = 2;
    int SHUTDOWN = 3;
    int CONNECT_SUCCESS = 4;
    int CONNECT_FAILED = 5;
    int AVAILABLE = 6;

    int RESTART = 7;
    int AUTO_STOP = 8;

    int ON_PACKET = 9;

    int NET_AVAILABLE = 10;
    int FOREGROUND = 11;
    int BACKGROUND = 12;

    int PULSE = 13;

    int TASK_START = -1;
    int TASK_ENQUEUE = -2;
    int TASK_SEND = -3;
    int TASK_SUCCESS = -4;
    int TASK_ERROR = -5;
    int TASK_CANCEL = -6;
    int TASK_TIME_OUT = -7;
}

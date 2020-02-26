package com.orientsec.easysocket.exception;

import java.util.Objects;

public class Event {
    public static final Event EMPTY = new Event(0, "");

    public static final Event SLEEP = new Event(1, "App is sleeping.");

    public static final Event SHUT_DOWN = new Event(2, "Connection shut down.");

    public static final Event NETWORK_NOT_AVAILABLE = new Event(3, "Network is not available.");

    public static final Event READ_IO_ERROR = new Event(101, "IO error in read looper.");

    public static final Event WRITE_IO_ERROR = new Event(102, "IO error in write looper.");

    public static final Event STREAM_SIZE_ERROR = new Event(103, "Invalid input stream size.");

    public static final Event PULSE_OVER_TIME = new Event(104, "Pulse over time.");

    public static final Event SOCKET_START_ERROR = new Event(104, "Fail to start a socket connect.");

    public static final Event TASK_REFUSED = new Event(1001, "Refuse to execute task.");

    public static final Event RESPONSE_TIME_OUT = new Event(1002, "Response time out.");

    public static Event unknown(String message) {
        if (message == null) message = "Unknown error!";
        return new Event(-1, message);
    }

    private int code;
    private String message;

    public Event(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event event = (Event) o;
        return getCode() == event.getCode() &&
                Objects.equals(getMessage(), event.getMessage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode(), getMessage());
    }

    @Override
    public String toString() {
        return "Event{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }


}

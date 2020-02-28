package com.orientsec.easysocket.exception;

/**
 * Product: EasySocket
 * Package: com.orientsec.easysocket.exception
 * Time: 2017/12/26 16:37
 * Author: Fredric
 * coding is art not science
 */

public class EasyException extends Exception {
    private Event event;

    public EasyException(Event event) {
        super(event.getMessage());
        this.event = event;
    }

    public EasyException(Event event, String message) {
        super(message);
        this.event = event;
    }

    public EasyException(int code, String message) {
        super(message);
        this.event = Event.create(code, message);
    }

    public EasyException(Event event, String message, Throwable cause) {
        super(message, cause);
        this.event = event;
    }

    public EasyException(int code, String message, Throwable cause) {
        super(message, cause);
        this.event = Event.create(code, message);
    }

    public EasyException(Event event, Throwable cause) {
        super(event.getMessage(), cause);
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}

package com.orientsec.easysocket.error;


import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Represents a custom exception for the EasySocket library.
 * This exception includes additional fields for error code and type,
 * along with utility methods for creating formatted exceptions.
 */
public class EasyException extends Exception {
    /**
     * The error code associated with this exception.
     */
    public final int code;

    /**
     * The error type associated with this exception.
     */
    public final int type;

    /**
     * Constructs an EasyException with the specified code, type, and message.
     *
     * @param code    The error code.
     * @param type    The error type.
     * @param message The detail message for the exception.
     */
    public EasyException(int code, int type, String message) {
        super(message);
        this.code = code;
        this.type = type;
    }

    /**
     * Constructs an EasyException with the specified code, type, message, and cause.
     *
     * @param code    The error code.
     * @param type    The error type.
     * @param message The detail message for the exception.
     * @param cause   The cause of the exception.
     */
    public EasyException(int code, int type, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.type = type;
    }

    /**
     * Returns a string representation of the exception, including its code, type, and message.
     *
     * @return A formatted string representation of the exception.
     */
    @NonNull
    @Override
    public String toString() {
        String message = getLocalizedMessage(); // Or getMessage()
        return String.format(Locale.getDefault(),
                "%s (Code: %d, Type: %d) %s",
                getClass().getName(),
                code,
                type,
                (message != null ? ": " + message : "")
        );
    }

    /**
     * Creates a new EasyException with a formatted message and a cause.
     *
     * @param code    The error code.
     * @param type    The error type.
     * @param message The base message for the exception.
     * @param suffix  Additional context to append to the message.
     * @param cause   The cause of the exception.
     * @return A new EasyException instance with the formatted message and cause.
     */
    public static EasyException create(int code, int type, String message, String suffix,
                                       Throwable cause) {
        String formattedMessage = String.format(Locale.getDefault(),
                "%s (%d, %d) {%s}",
                message,
                type,
                code,
                suffix
        );
        return new EasyException(code, type, formattedMessage, cause);
    }

    /**
     * Creates a new EasyException with a formatted message.
     *
     * @param code    The error code.
     * @param type    The error type.
     * @param message The base message for the exception.
     * @param suffix  Additional context to append to the message.
     * @return A new EasyException instance with the formatted message.
     */
    public static EasyException create(int code, int type, String message, String suffix) {
        String formattedMessage = String.format(Locale.getDefault(),
                "%s (%d, %d) {%s}",
                message,
                type,
                code,
                suffix
        );
        return new EasyException(code, type, formattedMessage);
    }
}
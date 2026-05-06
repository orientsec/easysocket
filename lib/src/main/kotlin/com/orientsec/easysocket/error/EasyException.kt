package com.orientsec.easysocket.error

import java.util.Locale

/**
 * Represents a custom exception for the EasySocket library.
 * This exception includes additional fields for error code and type,
 * along with utility methods for creating formatted exceptions.
 */
open class EasyException : Exception {
    /**
     * The error code associated with this exception.
     */
    val code: Int

    /**
     * The error type associated with this exception.
     */
    val type: Int

    /**
     * Constructs an EasyException with the specified code, type, and message.
     *
     * @param code    The error code.
     * @param type    The error type.
     * @param message The detail message for the exception.
     */
    constructor(code: Int, type: Int, message: String?) : super(message) {
        this.code = code
        this.type = type
    }

    /**
     * Constructs an EasyException with the specified code, type, message, and cause.
     *
     * @param code    The error code.
     * @param type    The error type.
     * @param message The detail message for the exception.
     * @param cause   The cause of the exception.
     */
    constructor(code: Int, type: Int, message: String?, cause: Throwable?) : super(message, cause) {
        this.code = code
        this.type = type
    }

    /**
     * Returns a string representation of the exception, including its code, type, and message.
     *
     * @return A formatted string representation of the exception.
     */
    override fun toString(): String {
        val message = localizedMessage // Or getMessage()
        return String.format(
            Locale.getDefault(),
            "%s (Code: %d, Type: %d) %s",
            javaClass.name,
            code,
            type,
            if (message != null) ": $message" else ""
        )
    }

    companion object {
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
        operator fun invoke(
            code: Int, type: Int, message: String, suffix: String,
            cause: Throwable?
        ): EasyException {
            val formattedMessage = String.format(
                Locale.getDefault(),
                "%s (%d, %d) {%s}",
                message,
                type,
                code,
                suffix
            )
            return EasyException(code, type, formattedMessage, cause)
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
        operator fun invoke(code: Int, type: Int, message: String, suffix: String): EasyException {
            val formattedMessage = String.format(
                Locale.getDefault(),
                "%s (%d, %d) {%s}",
                message,
                type,
                code,
                suffix
            )
            return EasyException(code, type, formattedMessage)
        }
    }
}

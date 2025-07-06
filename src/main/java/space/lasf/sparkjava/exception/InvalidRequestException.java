package space.lasf.sparkjava.exception;

/**
 * Exception thrown for requests that are invalid due to client error (e.g., bad syntax, missing parameters).
 * This typically maps to an HTTP 400 Bad Request status.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
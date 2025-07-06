package space.lasf.sparkjava.exception;

/**
 * Exception thrown when the server has a configuration problem that prevents it from fulfilling a request.
 */
public class ServerConfigurationException extends RuntimeException {
    public ServerConfigurationException(String message) {
        super(message);
    }
}
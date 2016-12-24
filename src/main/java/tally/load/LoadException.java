package tally.load;

public class LoadException extends Exception {
    private static final long serialVersionUID = 1L;

    public LoadException(String message) {
        super(message);
    }

    public LoadException(String message, Throwable exception) {
        super(message, exception);
    }
}

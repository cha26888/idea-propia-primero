package ideapropiaprimero.integration.ai;

public class AiException extends RuntimeException {

    private final int status;

    public AiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}

package gt.edu.uvg.ideapropiaprimero.exception;
public class RegistrationException extends RuntimeException {
    private final String field;
    public RegistrationException(String message) { this(null, message); }
    public RegistrationException(String field, String message) { super(message); this.field = field; }
    public String getField() { return field; }
}

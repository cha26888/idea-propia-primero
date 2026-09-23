package gt.edu.uvg.ideapropiaprimero.service;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    private final int minLength;
    public PasswordPolicy(@Value("${app.auth.password-min-length:4}") int minLength) {
        if (minLength < 4 || minLength > 64) throw new IllegalArgumentException("El mínimo debe estar entre 4 y 64.");
        this.minLength = minLength;
    }
    public int getMinLength() { return minLength; }
    public String error(String password) {
        if (password == null || password.isBlank()) return "Escribe tu contraseña.";
        if (password.codePointCount(0, password.length()) < minLength)
            return "Usa al menos " + minLength + " caracteres.";
        // BCrypt admite hasta 72 bytes; no truncamos silenciosamente.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            return "La contraseña es demasiado larga (máximo 72 bytes UTF-8).";
        return null;
    }
}

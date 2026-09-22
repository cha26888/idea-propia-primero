package gt.edu.uvg.ideapropiaprimero.dto;

import jakarta.validation.constraints.*;

public class RegisterForm {
    @NotBlank(message = "Escribe tu usuario.")
    @Pattern(regexp = "[A-Za-z0-9_.-]{3,30}", message = "Usa de 3 a 30 letras sin tildes, números, puntos, guiones o guiones bajos.")
    private String username = "";
    @NotBlank(message = "Escribe tu correo.")
    @Email(message = "Escribe un correo válido.")
    @Size(max = 254, message = "El correo es demasiado largo.")
    private String email = "";
    @NotBlank(message = "Escribe tu contraseña.")
    private String password = "";
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? "" : username.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? "" : email.strip(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password == null ? "" : password; }
}

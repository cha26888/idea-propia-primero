package ideapropiaprimero.service;
import ideapropiaprimero.dto.RegisterForm;
import ideapropiaprimero.exception.RegistrationException;
import ideapropiaprimero.model.Role;
import ideapropiaprimero.repository.UserRepository;
import jakarta.validation.Validator;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final Validator validator;
    public RegistrationService(UserRepository users, PasswordEncoder encoder, PasswordPolicy policy, Validator validator) {
        this.users = users; this.encoder = encoder; this.policy = policy; this.validator = validator;
    }
    
    public void registerStudent(RegisterForm form) { create(form, Role.STUDENT); }
    /**
     * Comprueba si el usuario y el correo ya están registrados.
     * Devuelve todos los errores encontrados.
     *
     * No crea usuarios ni modifica la base de datos.
     */
    public Map<String, String> checkDuplicates(RegisterForm form) {

        Map<String, String> errors = new LinkedHashMap<>();

        // Comprobar si el usuario ya existe
        if (users.findByLogin(form.getUsername()).isPresent()) {

            errors.put(
                "username",
                "Ese usuario ya está ocupado. Elige otro apodo."
            );

        }

        // Comprobar si el correo ya existe
        if (users.findByLogin(form.getEmail()).isPresent()) {

            errors.put(
                "email",
                "Ese correo ya tiene una cuenta. Inicia sesión o utiliza otro correo."
            );

        }

        return errors;
    }
    /** Solo utilizado por el aprovisionamiento local, nunca desde el formulario público. */
    public void provisionAuxiliar(RegisterForm form) { create(form, Role.AUXILIAR); }
    private void create(RegisterForm form, Role role) {
        if (!validator.validate(form).isEmpty()) throw new RegistrationException("Revisa el usuario y el correo.");
        String error = policy.error(form.getPassword());
        if (error != null) throw new RegistrationException(error);
        if (users.findByLogin(form.getUsername()).isPresent())
            throw new RegistrationException("username", "Ese usuario ya está ocupado. Elige otro apodo.");
        if (users.findByLogin(form.getEmail()).isPresent())
            throw new RegistrationException("email", "Ese correo ya tiene una cuenta. Inicia sesión o utiliza otro correo.");
        users.insert(form.getUsername(), form.getEmail(), encoder.encode(form.getPassword()), role);
    }
}

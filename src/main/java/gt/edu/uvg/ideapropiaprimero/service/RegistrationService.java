package gt.edu.uvg.ideapropiaprimero.service;
import gt.edu.uvg.ideapropiaprimero.dto.RegisterForm;
import gt.edu.uvg.ideapropiaprimero.exception.RegistrationException;
import gt.edu.uvg.ideapropiaprimero.model.Role;
import gt.edu.uvg.ideapropiaprimero.repository.UserRepository;
import jakarta.validation.Validator;
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

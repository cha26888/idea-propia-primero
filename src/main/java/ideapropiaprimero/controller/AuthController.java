package ideapropiaprimero.controller;

import ideapropiaprimero.dto.RegisterForm;
import ideapropiaprimero.exception.RegistrationException;
import ideapropiaprimero.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final RegistrationService registration;
    private final PasswordPolicy policy;

    public AuthController(RegistrationService registration, PasswordPolicy policy) {
        this.registration = registration;
        this.policy = policy;
    }

    @InitBinder("form")
    void allowedFields(WebDataBinder binder) {
        binder.setAllowedFields("username", "email", "password");
    }

    @ModelAttribute("passwordMin")
    public int passwordMin() {
        return policy.getMinLength();
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    private void registerIfValid(RegisterForm form, BindingResult errors) {

        // 1. Validar la política de contraseña
        String passwordError = policy.error(form.getPassword());

        if (passwordError != null && !errors.hasFieldErrors("password")) {

            errors.rejectValue(
                "password",
                "policy",
                passwordError
            );

        }

        // 2. Comprobar duplicados en campos con formato válido
        Map<String, String> duplicates = registration.checkDuplicates(form);

        for (Map.Entry<String, String> entry : duplicates.entrySet()) {

            String field = entry.getKey();

            // Evitar agregar errores repetidos al mismo campo
            if (!errors.hasFieldErrors(field)) {

                errors.rejectValue(
                    field,
                    "registration",
                    entry.getValue()
                );

            }

        }

        // 3. Si existen errores, no registrar al usuario
        if (errors.hasErrors()) {
            return;
        }

        // 4. Intentar crear la cuenta
        try {

            registration.registerStudent(form);

        } catch (RegistrationException e) {

            if (e.getField() == null) {

                errors.reject(
                    "registration",
                    e.getMessage()
                );

            } else {

                errors.rejectValue(
                    e.getField(),
                    "registration",
                    e.getMessage()
                );

            }

        }

    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult errors) {
        registerIfValid(form, errors);
        form.setPassword("");
        return errors.hasErrors() ? "auth/register" : "redirect:/login?registered";
    }

    /** Nunca serializar el formulario, BindingResult ni sus valores rechazados. */
    public record RegistrationResponse(boolean success, String message, Map<String, String> errors) {
    }

    @PostMapping(value = "/register/submit", produces = "application/json")
    @ResponseBody
    public ResponseEntity<RegistrationResponse> registerJson(@Valid @ModelAttribute("form") RegisterForm form,
            BindingResult errors) {
        registerIfValid(form, errors);
        form.setPassword("");
        if (!errors.hasErrors())
            return ResponseEntity.ok(new RegistrationResponse(true, "Cuenta creada.", Map.of()));
        Map<String, String> fields = new LinkedHashMap<>();
        errors.getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        String message = errors.hasGlobalErrors() ? errors.getGlobalErrors().get(0).getDefaultMessage()
                : "Revisa los campos indicados. Tus datos siguen en el formulario.";
        return ResponseEntity.badRequest().body(new RegistrationResponse(false, message, fields));
    }
}

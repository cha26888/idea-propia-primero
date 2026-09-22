package gt.edu.uvg.ideapropiaprimero.controller;

import gt.edu.uvg.ideapropiaprimero.dto.RegisterForm;
import gt.edu.uvg.ideapropiaprimero.exception.RegistrationException;
import gt.edu.uvg.ideapropiaprimero.service.*;
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
        this.registration = registration; this.policy = policy;
    }
    @InitBinder("form") void allowedFields(WebDataBinder binder) {
        binder.setAllowedFields("username", "email", "password");
    }
    @ModelAttribute("passwordMin") public int passwordMin() { return policy.getMinLength(); }
    @GetMapping("/login") public String login() { return "auth/login"; }
    @GetMapping("/register") public String register(Model model) {
        model.addAttribute("form", new RegisterForm()); return "auth/register";
    }
    private void registerIfValid(RegisterForm form, BindingResult errors) {
        String error = policy.error(form.getPassword());
        if (error != null && !errors.hasFieldErrors("password")) errors.rejectValue("password", "policy", error);
        if (errors.hasErrors()) return;
        try { registration.registerStudent(form); }
        catch (RegistrationException e) {
            if (e.getField() == null) errors.reject("registration", e.getMessage());
            else errors.rejectValue(e.getField(), "registration", e.getMessage());
        }
    }
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult errors) {
        registerIfValid(form, errors);
        form.setPassword("");
        return errors.hasErrors() ? "auth/register" : "redirect:/login?registered";
    }
    /** Nunca serializar el formulario, BindingResult ni sus valores rechazados. */
    public record RegistrationResponse(boolean success, String message, Map<String, String> errors) { }
    @PostMapping(value = "/register/submit", produces = "application/json")
    @ResponseBody
    public ResponseEntity<RegistrationResponse> registerJson(@Valid @ModelAttribute("form") RegisterForm form, BindingResult errors) {
        registerIfValid(form, errors);
        form.setPassword("");
        if (!errors.hasErrors()) return ResponseEntity.ok(new RegistrationResponse(true, "Cuenta creada.", Map.of()));
        Map<String,String> fields = new LinkedHashMap<>();
        errors.getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        String message = errors.hasGlobalErrors() ? errors.getGlobalErrors().get(0).getDefaultMessage()
                : "Revisa los campos indicados. Tus datos siguen en el formulario.";
        return ResponseEntity.badRequest().body(new RegistrationResponse(false, message, fields));
    }
}

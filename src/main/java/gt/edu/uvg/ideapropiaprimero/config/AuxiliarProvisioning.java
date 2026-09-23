package gt.edu.uvg.ideapropiaprimero.config;
import gt.edu.uvg.ideapropiaprimero.dto.RegisterForm;
import gt.edu.uvg.ideapropiaprimero.service.RegistrationService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Aprovisionamiento explícito. Una colisión falla; nunca convierte estudiantes en auxiliares. */
@Configuration
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class AuxiliarProvisioning {
    @Bean ApplicationRunner provisionAuxiliar(RegistrationService registration, Environment env) {
        return args -> {
            RegisterForm form = new RegisterForm();
            form.setUsername(env.getRequiredProperty("AUX_USERNAME"));
            form.setEmail(env.getRequiredProperty("AUX_EMAIL"));
            form.setPassword(env.getRequiredProperty("AUX_PASSWORD"));
            registration.provisionAuxiliar(form);
        };
    }
}

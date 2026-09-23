package ideapropiaprimero.integration;

import ideapropiaprimero.dto.RegisterForm;
import ideapropiaprimero.model.Role;
import ideapropiaprimero.repository.UserRepository;
import ideapropiaprimero.service.RegistrationService;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {
    static final Path DB;
    static { try { DB = Files.createTempDirectory("auth-test-").resolve("users.db"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.database.path", () -> DB.toString());
        r.add("app.bootstrap.enabled", () -> "false");
        r.add("app.auth.password-min-length", () -> "4");
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired RegistrationService registration;

    @Test void registerLoginByBothIdentifiersAndLogout() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username","Luna")
                .param("email","luna@example.com").param("password","abcd").param("role","AUXILIAR"))
            .andExpect(redirectedUrl("/login?registered"));
        var account = users.findByLogin("LUNA").orElseThrow();
        assertThat(account.getRole()).isEqualTo(Role.STUDENT);
        assertThat(account.getPasswordHash()).isNotEqualTo("abcd");
        assertThat(encoder.matches("abcd", account.getPasswordHash())).isTrue();
        // Una nueva instancia y conexión siguen leyendo los datos del archivo.
        assertThat(new UserRepository(DB.toString()).findByLogin("luna@example.com")).isPresent();
        for (String login : new String[]{"luna", "LUNA@EXAMPLE.COM"}) {
            var result = mvc.perform(post("/login").with(csrf()).param("username",login).param("password","abcd"))
                .andExpect(authenticated().withUsername("Luna")).andExpect(redirectedUrl("/welcome")).andReturn();
            MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
            mvc.perform(get("/welcome").session(session)).andExpect(status().isOk());
            mvc.perform(get("/admin").session(session)).andExpect(status().isForbidden());
            mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"));
            assertThat(session.isInvalid()).isTrue();
        }
    }
    @Test void duplicatesAreRejectedIgnoringCase() throws Exception {
        RegisterForm form = new RegisterForm(); form.setUsername("Sol"); form.setEmail("sol@example.com"); form.setPassword("abcd");
        registration.registerStudent(form);
        mvc.perform(post("/register").with(csrf()).param("username","SOL").param("email","other@example.com").param("password","abcd"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        mvc.perform(post("/register").with(csrf()).param("username","Other").param("email","SOL@example.com").param("password","abcd"))
            .andExpect(model().attributeHasErrors("form"));
        assertThat(users.findByLogin("Other")).isEmpty();
    }
    @Test void invalidInputAndCsrfAreRejected() throws Exception {
        mvc.perform(post("/register").param("username","NoCsrf").param("email","csrf@example.com").param("password","abcd"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf()).param("username","bad@name").param("email","invalid").param("password","abc"))
            .andExpect(model().attributeHasFieldErrors("form","username","email","password"));
        assertThat(users.findByLogin("NoCsrf")).isEmpty();
    }
    @Test void privateRoutesAndWrongCredentials() throws Exception {
        mvc.perform(get("/welcome")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").with(csrf()).param("username","unknown").param("password","wrong"))
            .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }
    @Test void provisionedAuxiliarCanAccessAdmin() throws Exception {
        RegisterForm form = new RegisterForm(); form.setUsername("AuxTest"); form.setEmail("aux@example.com"); form.setPassword("abcd");
        registration.provisionAuxiliar(form);
        var result = mvc.perform(post("/login").with(csrf()).param("username","AuxTest").param("password","abcd"))
            .andExpect(authenticated().withRoles("AUXILIAR")).andReturn();
        mvc.perform(get("/admin").session((MockHttpSession) result.getRequest().getSession(false))).andExpect(status().isOk());
    }

    @Test void jsonErrorsIdentifyFieldAndNeverReturnPassword() throws Exception {
        mvc.perform(post("/register/submit").accept("application/json").with(csrf())
                .param("username","JsonUser").param("email","json@example.com").param("password","abcd"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(post("/register/submit").accept("application/json").with(csrf())
                .param("username","DifferentUser").param("email","JSON@example.com").param("password","secret123"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").isString())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret123"))));
        mvc.perform(post("/register/submit").accept("application/json").with(csrf())
                .param("username","JSONUSER").param("email","new@example.com").param("password","abcd"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.username").isString());
        mvc.perform(post("/register/submit").accept("application/json").with(csrf())
                .param("username","ShortPwd").param("email","short@example.com").param("password","abc"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").isString());
    }

    @Test void jsonEndpointRequiresCsrfAndFormPointsToIt() throws Exception {
        mvc.perform(post("/register/submit").accept("application/json")
                .param("username","CsrfGuard").param("email","guard@example.com").param("password","abcd"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/register")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-submit-url=\"/register/submit\"")));
    }
}

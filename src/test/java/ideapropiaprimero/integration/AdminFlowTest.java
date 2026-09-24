package ideapropiaprimero.integration;

import ideapropiaprimero.model.Role;
import ideapropiaprimero.repository.UserRepository;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminFlowTest {
    static final Path DB;
    static { try { DB = Files.createTempDirectory("admin-test-").resolve("users.db"); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.database.path", () -> DB.toString());
        r.add("app.bootstrap.enabled", () -> "false");
        r.add("app.auth.password-min-length", () -> "4");
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @Test void auxiliarLogsInAndSeesStudentListWithoutSecrets() throws Exception {
        String studentHash = encoder.encode("abcd");
        String auxHash = encoder.encode("abcd");
        users.insert("Mora", "mora@example.com", studentHash, Role.STUDENT);
        users.insert("Rio", "rio@example.com", encoder.encode("abcd"), Role.STUDENT);
        users.insert("AuxLista", "auxlista@example.com", auxHash, Role.AUXILIAR);
        var result = mvc.perform(post("/login").with(csrf()).param("username", "AuxLista").param("password", "abcd"))
            .andExpect(authenticated().withRoles("AUXILIAR")).andExpect(redirectedUrl("/welcome")).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/admin").session(session))
            .andExpect(status().isOk()).andExpect(view().name("admin/index"))
            .andExpect(model().attribute("students", hasItem(hasProperty("username", is("Mora")))))
            .andExpect(content().string(allOf(containsString("Mora"), containsString("mora@example.com"),
                containsString("Rio"), containsString("rio@example.com"))))
            .andExpect(content().string(allOf(not(containsString("auxlista@example.com")),
                not(containsString(studentHash)), not(containsString(auxHash)))));
    }
    @Test void studentsAndAnonymousCannotSeeStudentList() throws Exception {
        mvc.perform(get("/admin").with(user("alumno").roles("STUDENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }
}

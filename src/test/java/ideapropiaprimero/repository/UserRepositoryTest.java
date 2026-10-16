package ideapropiaprimero.repository;

import ideapropiaprimero.dto.StudentSummary;
import ideapropiaprimero.model.Role;
import java.nio.file.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class UserRepositoryTest {
    @TempDir Path dir;
    UserRepository users;

    @BeforeEach void setUp() { users = new UserRepository(dir.resolve("users.db").toString()); }

    @Test void emptyDatabaseHasNoStudents() {
        assertThat(users.findAllStudents()).isEmpty();
    }
    @Test void findAllStudentsReturnsOnlyStudentsOrderedByUsername() {
        users.insert("zeta", "zeta@example.com", "hash-z", Role.STUDENT);
        users.insert("AuxUno", "aux@example.com", "hash-aux", Role.AUXILIAR);
        users.insert("Beto", "beto@example.com", "hash-b", Role.STUDENT);
        users.insert("ana", "ana@example.com", "hash-a", Role.STUDENT);
        assertThat(users.findAllStudents()).extracting(StudentSummary::getUsername).containsExactly("ana", "Beto", "zeta");
    }
    @Test void studentSummaryKeepsStoredDataAndCreationDate() {
        users.insert("Luna", "Luna@Example.com", "hash-l", Role.STUDENT);
        StudentSummary student = users.findAllStudents().getFirst();
        assertThat(student.getId()).isEqualTo(users.findByLogin("luna").orElseThrow().getId());
        assertThat(student.getEmail()).isEqualTo("Luna@Example.com");
        assertThat(student.getCreatedAt()).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(1));
    }
}

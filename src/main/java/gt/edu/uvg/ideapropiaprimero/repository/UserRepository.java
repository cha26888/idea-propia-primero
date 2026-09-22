package gt.edu.uvg.ideapropiaprimero.repository;

import gt.edu.uvg.ideapropiaprimero.model.*;
import gt.edu.uvg.ideapropiaprimero.exception.RegistrationException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final String url;
    public UserRepository(@Value("${app.database.path:data/idea-propia-primero.db}") String path) {
        try {
            Path file = Path.of(path).toAbsolutePath();
            Files.createDirectories(file.getParent());
            url = "jdbc:sqlite:" + file;
            try (Connection c = connect(); Statement st = c.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL,
                        username_key TEXT NOT NULL UNIQUE,
                        email TEXT NOT NULL,
                        email_key TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL CHECK(role IN ('STUDENT','AUXILIAR')),
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            }
        } catch (Exception e) { throw new IllegalStateException("No se pudo inicializar SQLite.", e); }
    }
    private Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) { s.execute("PRAGMA busy_timeout=5000"); }
        catch (SQLException e) { c.close(); throw e; }
        return c;
    }
    public static String key(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    public Optional<User> findByLogin(String login) {
        if (login == null) return Optional.empty();
        // Los usuarios no contienen @: no puede confundirse un usuario con un correo.
        String column = login.contains("@") ? "email_key" : "username_key";
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement("SELECT * FROM users WHERE " + column + " = ?")) {
            s.setString(1, key(login));
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new User(r.getLong("id"), r.getString("username"), r.getString("email"),
                        r.getString("password_hash"), Role.valueOf(r.getString("role"))));
            }
        } catch (SQLException e) { throw new IllegalStateException("No se pudo consultar la cuenta.", e); }
    }
    public void insert(String username, String email, String hash, Role role) {
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO users(username,username_key,email,email_key,password_hash,role) VALUES(?,?,?,?,?,?)")) {
            s.setString(1, username); s.setString(2, key(username));
            s.setString(3, email); s.setString(4, key(email)); s.setString(5, hash); s.setString(6, role.name());
            s.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) throw new RegistrationException("Ese usuario o correo ya está registrado. Usa otro o inicia sesión.");
            throw new IllegalStateException("No se pudo guardar la cuenta.", e);
        }
    }
}

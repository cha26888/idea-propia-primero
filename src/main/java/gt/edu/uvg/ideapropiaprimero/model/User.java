package gt.edu.uvg.ideapropiaprimero.model;

/** Cuenta persistente. Nunca exponer passwordHash en vistas o respuestas. */
public class User {
    private final long id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final Role role;
    public User(long id, String username, String email, String passwordHash, Role role) {
        this.id = id; this.username = username; this.email = email;
        this.passwordHash = passwordHash; this.role = role;
    }
    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}

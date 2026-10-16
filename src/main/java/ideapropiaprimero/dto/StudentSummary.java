package ideapropiaprimero.dto;

import java.time.LocalDateTime;

public class StudentSummary {
    private final long id;
    private final String username;
    private final String email;
    private final LocalDateTime createdAt;
    public StudentSummary(long id, String username, String email, LocalDateTime createdAt) {
        this.id = id; this.username = username; this.email = email; this.createdAt = createdAt;
    }
    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

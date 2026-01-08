package ch.heig.motd.model;

import java.time.Instant;

public class User {
    private final long id;
    private final String username;
    private final String passwordHash;
    private final Instant createdAt;

    public User(long id, String username, String passwordHash, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}

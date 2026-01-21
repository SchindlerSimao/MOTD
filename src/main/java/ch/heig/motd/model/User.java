package ch.heig.motd.model;

import java.time.Instant;

/**
 * User model representing a user in the system.
 */
public class User {
    /**
     * Unique identifier of the user.
     */
    private final long id;
    /**
     * Username of the user.
     */
    private final String username;

    /**
     * Password hash of the user.
     */
    private final String passwordHash;

    /**
     * Instant when the user was created.
     */
    private final Instant createdAt;

    /**
     * Constructor.
     *
     * @param id           id of the user
     * @param username     username of the user
     * @param passwordHash password hash of the user
     * @param createdAt    instant when the user was created
     */
    public User(long id, String username, String passwordHash, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    /**
     * Gets the id of the user.
     *
     * @return id of the user
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the username of the user.
     * @return username of the user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password hash of the user.
     * @return password hash of the user
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Gets the instant when the user was created.
     * @return instant when the user was created
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}

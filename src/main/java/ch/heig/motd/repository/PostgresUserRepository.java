package ch.heig.motd.repository;

import ch.heig.motd.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;

/**
 * PostgreSQL implementation of UserRepository.
 */
public class PostgresUserRepository implements UserRepository {
    /**
     * Logger instance for logging.
     */
    private static final Logger log = LoggerFactory.getLogger(PostgresUserRepository.class);

    /**
     * Data source for database connections.
     */
    private final DataSource ds;

    /**
     * Constructor.
     * @param ds data source
     */
    public PostgresUserRepository(DataSource ds) { this.ds = ds; }

    @Override
    public Optional<User> findById(long id) {
        log.debug("Finding user by id {}", id);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, username, password_hash, created_at FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
            return Optional.empty();
        } catch (SQLException e) { log.error("Error finding user {}", id, e); throw new RuntimeException(e); }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username {}", username);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, username, password_hash, created_at FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
            return Optional.empty();
        } catch (SQLException e) { log.error("Error finding user by username {}", username, e); throw new RuntimeException(e); }
    }

    @Override
    public User save(String username, String passwordHash) {
        log.debug("Saving user {}", username);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO users(username, password_hash, created_at) VALUES (?, ?, now())", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return new User(rs.getLong(1), username, passwordHash, Instant.now());
            throw new RuntimeException("no id generated");
        } catch (SQLException e) { log.error("Error saving user {}", username, e); throw new RuntimeException(e); }
    }

    @Override
    public void delete(long id) {
        log.debug("Deleting user {}", id);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Error deleting user {}", id, e); throw new RuntimeException(e); }
    }

    /**
     * Maps a ResultSet row to a User object.
     * @param rs result set
     * @return mapped user
     * @throws SQLException if a database error occurs
     */
    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"), rs.getTimestamp("created_at").toInstant());
    }
}

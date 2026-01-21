package ch.heig.motd.service;

import ch.heig.motd.model.User;
import ch.heig.motd.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Implementation of the UserService interface using PostgreSQL.
 */
public class UserServicePostgres implements UserService {
    /**
     * Logger for the UserServicePostgres class.
     */
    private static final Logger log = LoggerFactory.getLogger(UserServicePostgres.class);

    /**
     * User repository for database operations.
     */
    private final UserRepository repo;

    /**
     * Constructor.
     * @param repo user repository
     */
    public UserServicePostgres(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public User register(String username, String password) {
        log.info("Register user: {}", username);
        if (repo.findByUsername(username).isPresent()) {
            log.warn("Username already exists: {}", username);
            throw new IllegalArgumentException("username.exists");
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        return repo.save(username, hash);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repo.findByUsername(username);
    }

    @Override
    public Optional<User> findById(long id) {
        return repo.findById(id);
    }

    @Override
    public boolean verifyPassword(User user, String password) {
        try {
            return BCrypt.checkpw(password, user.getPasswordHash());
        } catch (Exception e) {
            log.error("Error verifying password for user {}", user.getUsername(), e);
            return false;
        }
    }

    @Override
    public void delete(long id) {
        log.info("Deleting user id={}", id);
        repo.delete(id);
    }
}

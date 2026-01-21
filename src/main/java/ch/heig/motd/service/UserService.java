package ch.heig.motd.service;

import ch.heig.motd.model.User;

import java.util.Optional;

/**
 * Service interface for User entity.
 */
public interface UserService {
    /**
     * Registers a new user.
     * @param username username
     * @param password password
     * @return the registered user
     */
    User register(String username, String password);

    /**
     * Finds a user by username.
     * @param username username
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by id.
     * @param id user id
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findById(long id);

    /**
     * Verifies a user's password.
     * @param user user
     * @param password password
     * @return true if the password is correct, false otherwise
     */
    boolean verifyPassword(User user, String password);

    /**
     * Deletes a user by id.
     * @param id user id
     */
    void delete(long id);
}

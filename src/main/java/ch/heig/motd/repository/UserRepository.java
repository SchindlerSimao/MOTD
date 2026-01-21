package ch.heig.motd.repository;

import ch.heig.motd.model.User;

import java.util.Optional;

/**
 * Repository interface for User entity.
 */
public interface UserRepository {
    /**
     * Finds a user by its id.
     * @param id user id
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findById(long id);

    /**
     * Finds a user by its username.
     * @param username username
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Saves a new user.
     * @param username username
     * @param passwordHash hashed password
     * @return the saved user
     */
    User save(String username, String passwordHash);

    /**
     * Deletes a user by its id.
     * @param id user id
     */
    void delete(long id);
}

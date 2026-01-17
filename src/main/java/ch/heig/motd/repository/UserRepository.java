package ch.heig.motd.repository;

import ch.heig.motd.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(long id);
    Optional<User> findByUsername(String username);
    User save(String username, String passwordHash);
    void delete(long id);
}

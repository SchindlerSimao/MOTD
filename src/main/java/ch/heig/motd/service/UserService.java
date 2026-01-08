package ch.heig.motd.service;

import ch.heig.motd.model.User;

import java.util.Optional;

public interface UserService {
    User register(String username, String password);
    Optional<User> findByUsername(String username);
    Optional<User> findById(long id);
    boolean verifyPassword(User user, String password);
    void delete(long id);
}

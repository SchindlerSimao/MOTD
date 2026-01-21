package ch.heig.motd.service;

import ch.heig.motd.model.User;
import ch.heig.motd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServicePostgresTest {
    private UserRepository repo;
    private UserServicePostgres service;

    @BeforeEach
    public void setup() {
        repo = mock(UserRepository.class);
        service = new UserServicePostgres(repo);
    }

    @Test
    public void register_existingUsername_throws() {
        when(repo.findByUsername("bob")).thenReturn(Optional.of(new User(1L, "bob", "h", Instant.now())));
        assertThrows(IllegalArgumentException.class, () -> service.register("bob", "pwd"));
    }

    @Test
    public void verifyPassword_trueFalse() {
        String pw = "secret";
        String hash = BCrypt.hashpw(pw, BCrypt.gensalt());
        User u = new User(2L, "joe", hash, Instant.now());
        assertTrue(service.verifyPassword(u, pw));
        assertFalse(service.verifyPassword(u, "bad"));
    }
}


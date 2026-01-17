package ch.heig.motd.service;

import ch.heig.motd.model.User;
import ch.heig.motd.repository.PostgresUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServicePostgresTest {
    private PostgresUserRepository repo;
    private UserServicePostgres service;

    @BeforeEach
    public void setup() {
        repo = mock(PostgresUserRepository.class);
        service = new UserServicePostgres(repo);
    }

    @Test
    public void register_newUser_savesWithHashedPassword() {
        when(repo.findByUsername("alice")).thenReturn(Optional.empty());
        var savedUser = new User(1L, "alice", "hashed", Instant.now());
        when(repo.save(eq("alice"), anyString())).thenReturn(savedUser);

        var result = service.register("alice", "password123");

        assertEquals(savedUser, result);
        verify(repo).save(eq("alice"), argThat(hash -> hash.startsWith("$2a$")));
    }

    @Test
    public void register_existingUser_throwsException() {
        var existingUser = new User(1L, "alice", "hashed", Instant.now());
        when(repo.findByUsername("alice")).thenReturn(Optional.of(existingUser));

        assertThrows(IllegalArgumentException.class, () -> service.register("alice", "password123"));
    }

    @Test
    public void findByUsername_delegatesToRepository() {
        var user = new User(1L, "alice", "hashed", Instant.now());
        when(repo.findByUsername("alice")).thenReturn(Optional.of(user));

        var result = service.findByUsername("alice");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    public void findById_delegatesToRepository() {
        var user = new User(1L, "alice", "hashed", Instant.now());
        when(repo.findById(1L)).thenReturn(Optional.of(user));

        var result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    public void verifyPassword_correctPassword_returnsTrue() {
        String password = "password123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        var user = new User(1L, "alice", hash, Instant.now());

        boolean result = service.verifyPassword(user, password);

        assertTrue(result);
    }

    @Test
    public void verifyPassword_incorrectPassword_returnsFalse() {
        String password = "password123";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        var user = new User(1L, "alice", hash, Instant.now());

        boolean result = service.verifyPassword(user, "wrongpassword");

        assertFalse(result);
    }

    @Test
    public void delete_delegatesToRepository() {
        service.delete(1L);

        verify(repo).delete(1L);
    }
}

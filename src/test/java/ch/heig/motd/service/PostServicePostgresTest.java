package ch.heig.motd.service;

import ch.heig.motd.model.Post;
import ch.heig.motd.model.User;
import ch.heig.motd.repository.PostRepository;
import ch.heig.motd.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PostServicePostgresTest {
    private PostRepository postRepo;
    private UserRepository userRepo;
    private PostServicePostgres service;

    @BeforeEach
    public void setup() {
        postRepo = mock(PostRepository.class);
        userRepo = mock(UserRepository.class);
        service = new PostServicePostgres(postRepo, userRepo);
    }

    @Test
    public void create_delegatesToRepository() {
        Post post = new Post(1L, 42L, "test", Instant.now(), LocalDate.now());
        when(userRepo.findById(42L)).thenReturn(Optional.of(new User(42L, "u", "h", Instant.now())));
        when(postRepo.save(42L, "test")).thenReturn(post);

        Post result = service.create(42L, "test");

        assertEquals(post, result);
        verify(postRepo).save(42L, "test");
    }

    @Test
    public void findById_delegatesToRepository() {
        Post post = new Post(1L, 42L, "test", Instant.now(), LocalDate.now());
        when(postRepo.findById(1L)).thenReturn(Optional.of(post));

        Optional<Post> result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(post, result.get());
    }

    @Test
    public void findAll_delegatesToRepository() {
        List<Post> posts = List.of(
            new Post(1L, 42L, "test1", Instant.now(), LocalDate.now()),
            new Post(2L, 43L, "test2", Instant.now(), LocalDate.now())
        );
        when(postRepo.findAll()).thenReturn(posts);

        List<Post> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals(posts, result);
    }

    @Test
    public void delete_delegatesToRepository() {
        service.delete(1L);

        verify(postRepo).delete(1L);
    }

    @Test
    public void updateContent_delegatesToRepository() {
        Post post = new Post(1L, 42L, "updated", Instant.now(), LocalDate.now());
        when(postRepo.updateContent(1L, "updated")).thenReturn(post);

        Post result = service.updateContent(1L, "updated");

        assertEquals(post, result);
        verify(postRepo).updateContent(1L, "updated");
    }
}

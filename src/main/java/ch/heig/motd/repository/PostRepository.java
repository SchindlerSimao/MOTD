package ch.heig.motd.repository;

import ch.heig.motd.model.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Post save(long authorId, String content);
    Optional<Post> findById(long id);
    List<Post> findAll();
    void delete(long id);
    Post updateContent(long id, String content);
}

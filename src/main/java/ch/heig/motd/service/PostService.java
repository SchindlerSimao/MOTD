package ch.heig.motd.service;

import ch.heig.motd.model.Post;

import java.util.List;
import java.util.Optional;

public interface PostService {
    Post create(long authorId, String content);
    Optional<Post> findById(long id);
    List<Post> findAll();
    void delete(long id);
    Post updateContent(long id, String newContent);
}

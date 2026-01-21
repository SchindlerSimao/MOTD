package ch.heig.motd.service;

import ch.heig.motd.model.Post;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Post entity.
 */
public interface PostService {
    /**
     * Creates a new post.
     * @param authorId author id
     * @param content post content
     * @return the created post
     */
    Post create(long authorId, String content);

    /**
     * Finds a post by its id.
     * @param id post id
     * @return an Optional containing the post if found, or empty if not found
     */
    Optional<Post> findById(long id);

    /**
     * Finds all posts.
     * @return list of all posts
     */
    List<Post> findAll();

    /**
     * Deletes a post by its id.
     * @param id post id
     */
    void delete(long id);

    /**
     * Updates the content of a post.
     * @param id post id
     * @param newContent new content
     * @return the updated post
     */
    Post updateContent(long id, String newContent);
}

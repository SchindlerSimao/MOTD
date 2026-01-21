package ch.heig.motd.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Post model representing a message of the day post.
 */
public class Post {
    /**
     * Unique identifier of the post.
     */
    private final long id;

    /**
     * Identifier of the author of the post.
     */
    private final long authorId;

    /**
     * Content of the post.
     */
    private String content;

    /**
     * Instant when the post was created.
     */
    private final Instant createdAt;

    /**
     * Date when the post should be displayed.
     */
    private LocalDate displayAt;

    /**
     * Constructor.
     *
     * @param id        id of the post
     * @param authorId  id of the author
     * @param content   content of the post
     * @param createdAt instant when the post was created
     * @param displayAt date when the post should be displayed
     */
    public Post(long id, long authorId, String content, Instant createdAt, LocalDate displayAt) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
        this.displayAt = displayAt;
    }

    /**
     * Gets the id of the post.
     *
     * @return id of the post
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the author id.
     *
     * @return id of the author
     */
    public long getAuthorId() {
        return authorId;
    }

    /**
     * Gets or sets the content of the post.
     *
     * @return content of the post
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content of the post.
     *
     * @param content new content of the post
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the creation instant of the post.
     *
     * @return instant when the post was created
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets or sets the display date of the post.
     *
     * @return date when the post should be displayed
     */
    public LocalDate getDisplayAt() {
        return displayAt;
    }
}

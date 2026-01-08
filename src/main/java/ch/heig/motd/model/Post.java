package ch.heig.motd.model;

import java.time.Instant;
import java.time.LocalDate;

public class Post {
    private final long id;
    private final long authorId;
    private String content;
    private final Instant createdAt;
    private LocalDate displayAt;

    public Post(long id, long authorId, String content, Instant createdAt, LocalDate displayAt) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
        this.displayAt = displayAt;
    }

    public long getId() { return id; }
    public long getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public LocalDate getDisplayAt() { return displayAt; }
    public void setDisplayAt(LocalDate displayAt) { this.displayAt = displayAt; }
}

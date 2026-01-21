package ch.heig.motd.repository;

import ch.heig.motd.model.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL implementation of PostRepository.
 */
public class PostgresPostRepository implements PostRepository {
    /**
     * Logger instance for logging.
     */
    private static final Logger log = LoggerFactory.getLogger(PostgresPostRepository.class);

    /**
     * Data source for database connections.
     */
    private final DataSource ds;

    /**
     * Constructor.
     * @param ds data source
     */
    public PostgresPostRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Post save(long authorId, String content) {
        log.debug("Saving post for author {}", authorId);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO posts(author_id, content, created_at, display_at) VALUES (?, ?, now(), current_date + 1) RETURNING id, created_at, display_at")) {
            ps.setLong(1, authorId);
            ps.setString(2, content);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("id");
                Instant created = rs.getTimestamp("created_at").toInstant();
                LocalDate displayAt = rs.getDate("display_at").toLocalDate();
                log.info("Post created id={} author={}", id, authorId);
                return new Post(id, authorId, content, created, displayAt);
            }
            log.error("Insert returned no rows");
            throw new RuntimeException("insert failed");
        } catch (SQLException e) {
            log.error("Error saving post for author {}", authorId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Post> findById(long id) {
        log.debug("Finding post by id {}", id);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, author_id, content, created_at, display_at FROM posts WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(map(rs));
            }
            return Optional.empty();
        } catch (SQLException e) { log.error("Error finding post {}", id, e); throw new RuntimeException(e); }
    }

    @Override
    public List<Post> findAll() {
        log.debug("Finding all posts");
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, author_id, content, created_at, display_at FROM posts ORDER BY created_at DESC")) {
            ResultSet rs = ps.executeQuery();
            List<Post> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { log.error("Error finding all posts", e); throw new RuntimeException(e); }
    }

    @Override
    public void delete(long id) {
        log.debug("Deleting post {}", id);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM posts WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { log.error("Error deleting post {}", id, e); throw new RuntimeException(e); }
    }

    @Override
    public Post updateContent(long id, String content) {
        log.debug("Updating post {} content", id);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE posts SET content = ? WHERE id = ? RETURNING id, author_id, content, created_at, display_at")) {
            ps.setString(1, content);
            ps.setLong(2, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
            throw new RuntimeException("not found");
        } catch (SQLException e) { log.error("Error updating post {}", id, e); throw new RuntimeException(e); }
    }

    /**
     * Maps a ResultSet row to a Post object.
     * @param rs result set
     * @return mapped Post
     * @throws SQLException if a database error occurs
     */
    private Post map(ResultSet rs) throws SQLException {
        return new Post(rs.getLong("id"), rs.getLong("author_id"), rs.getString("content"), rs.getTimestamp("created_at").toInstant(), rs.getDate("display_at").toLocalDate());
    }
}

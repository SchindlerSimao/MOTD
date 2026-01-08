package ch.heig.motd.service;

import ch.heig.motd.model.Post;
import ch.heig.motd.repository.PostgresPostRepository;
import ch.heig.motd.repository.PostgresUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class PostServicePostgres implements PostService {
    private static final Logger log = LoggerFactory.getLogger(PostServicePostgres.class);
    private final PostgresPostRepository postRepo;
    private final PostgresUserRepository userRepo;

    public PostServicePostgres(PostgresPostRepository postRepo, PostgresUserRepository userRepo) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
    }

    @Override
    public Post create(long authorId, String content) {
        log.info("Create post by user {}", authorId);
        return postRepo.save(authorId, content);
    }

    @Override
    public Optional<Post> findById(long id) { return postRepo.findById(id); }

    @Override
    public List<Post> findAll() { return postRepo.findAll(); }

    @Override
    public void delete(long id) {
        log.info("Delete post {}", id);
        postRepo.delete(id);
    }

    @Override
    public Post updateContent(long id, String newContent) {
        log.info("Update post {} content", id);
        return postRepo.updateContent(id, newContent);
    }
}

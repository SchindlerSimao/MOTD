package ch.heig.motd.service;

import ch.heig.motd.model.Post;
import ch.heig.motd.model.User;
import ch.heig.motd.repository.PostRepository;
import ch.heig.motd.repository.UserRepository;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class PostServicePostgres implements PostService {
    private static final Logger log = LoggerFactory.getLogger(PostServicePostgres.class);
    private final PostRepository postRepo;
    private final UserRepository userRepo;

    public PostServicePostgres(PostRepository postRepo, UserRepository userRepo) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
    }

    @Override
    public Post create(long authorId, String content) {
        log.info("Create post by user {}", authorId);
        // Vérifier que l'utilisateur existe
        Optional<User> userOpt = userRepo.findById(authorId);
        if (userOpt.isEmpty()) {
            throw new NotFoundResponse("user not found");
        }
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

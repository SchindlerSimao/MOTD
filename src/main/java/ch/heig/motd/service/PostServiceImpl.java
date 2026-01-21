package ch.heig.motd.service;

import ch.heig.motd.model.Post;
import ch.heig.motd.model.User;
import ch.heig.motd.repository.PostRepository;
import ch.heig.motd.repository.UserRepository;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of PostService.
 */
public class PostServiceImpl implements PostService {
    /**
     * Logger instance for logging.
     */
    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    /**
     * Post repository for data access.
     */
    private final PostRepository postRepo;

    /**
     * User repository for data access.
     */
    private final UserRepository userRepo;

    /**
     * Constructor.
     * @param postRepo post repository
     * @param userRepo user repository
     */
    public PostServiceImpl(PostRepository postRepo, UserRepository userRepo) {
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
    public List<Post> findByDate(LocalDate date) { return postRepo.findByDisplayDate(date); }

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

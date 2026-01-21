package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.dto.PostDto;
import ch.heig.motd.model.Post;
import ch.heig.motd.service.AuthService;
import io.javalin.http.NotFoundResponse;
import ch.heig.motd.service.PostService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for managing posts.
 */
public class PostController {
    /**
     * Logger instance for logging.
     */
    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    /**
     * Post service for handling post operations.
     */
    private final PostService postService;

    /**
     * Authentication service for handling authentication.
     */
    private final AuthService authService;

    /**
     * Constructor.
     * @param postService post service
     * @param authService authentication service
     */
    public PostController(PostService postService, AuthService authService) {
        this.postService = postService;
        this.authService = authService;
    }

    /**
     * Lists all existing posts.
     * @param ctx Javalin context
     */
    public void list(Context ctx) {
        try {
            List<Post> posts = postService.findAll();
            List<Map<String, Object>> out = posts.stream().map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("authorId", p.getAuthorId());
                m.put(ApiConstants.Keys.CONTENT, p.getContent());
                m.put("createdAt", p.getCreatedAt().toString());
                m.put("displayAt", p.getDisplayAt().toString());
                return m;
            }).collect(Collectors.toList());
            ctx.json(out);
        } catch (Exception e) {
            log.error("Unexpected error in list posts", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    /**
     * Creates a new post.
     * @param ctx Javalin context
     */
    public void create(Context ctx) {
        try {
            // requireAuth must have set uid attribute; enforce centralized auth
            Long uid = ctx.attribute("uid");
            if (uid == null) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.UNAUTHORIZED)); return; }

            String content;
            try {
                PostDto newPost = ctx.bodyAsClass(PostDto.class);
                if (newPost == null || newPost.content() == null) {
                    ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT));
                    return;
                }
                content = newPost.content();
            } catch (Exception e) {
                // strict API: reject malformed or non-conforming JSON
                log.warn("Failed to parse body as PostDto: {}", e.getMessage());
                ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT));
                return;
            }

            if (content == null || content.isBlank()) { ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT)); return; }
            Post p = postService.create(uid, content);
            Map<String, Object> out = new HashMap<>();
            out.put("id", p.getId());
            out.put(ApiConstants.Keys.CONTENT, p.getContent());
            out.put("authorId", p.getAuthorId());
            ctx.status(201).json(out);
        } catch (NotFoundResponse e) {
            ctx.status(404).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.NOT_FOUND));
        } catch (Exception e) {
            log.error("Unexpected error in create post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    /**
     * Updates an existing post.
     * @param ctx Javalin context
     */
    public void update(Context ctx) {
        try {
            Long uid = ctx.attribute("uid");
            if (uid == null) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.UNAUTHORIZED)); return; }
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Post> op = postService.findById(id);
            if (op.isEmpty()) { ctx.status(404).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.NOT_FOUND)); return; }
            Post p = op.get();
            if (p.getAuthorId() != uid) { ctx.status(403).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.FORBIDDEN)); return; }
            Map body = ctx.bodyAsClass(Map.class);
            String content = (String) body.get(ApiConstants.Keys.CONTENT);
            if (content == null || content.isBlank()) { ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT)); return; }
            p = postService.updateContent(id, content);
            Map<String, Object> out = new HashMap<>();
            out.put("id", p.getId());
            out.put(ApiConstants.Keys.CONTENT, p.getContent());
            ctx.json(out);
        } catch (Exception e) {
            log.error("Unexpected error in update post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    /**
     * Deletes an existing post.
     * @param ctx Javalin context
     */
    public void delete(Context ctx) {
        try {
            Long uid = ctx.attribute("uid");
            if (uid == null) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.UNAUTHORIZED)); return; }
            long id = Long.parseLong(ctx.pathParam("id"));
            Optional<Post> op = postService.findById(id);
            if (op.isEmpty()) { ctx.status(404).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.NOT_FOUND)); return; }
            Post p = op.get();
            if (p.getAuthorId() != uid) { ctx.status(403).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.FORBIDDEN)); return; }
            postService.delete(id);
            ctx.status(204);
        } catch (Exception e) {
            log.error("Unexpected error in delete post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }
}

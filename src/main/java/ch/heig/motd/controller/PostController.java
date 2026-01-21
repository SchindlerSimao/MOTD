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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostController {
    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;
    private final AuthService authService;

    public PostController(PostService postService, AuthService authService) {
        this.postService = postService;
        this.authService = authService;
    }

    public void list(Context ctx) {
        try {
            List<Post> posts = postService.findAll();
            List out = posts.stream().map(p -> Map.of(
                    "id", p.getId(),
                    "authorId", p.getAuthorId(),
                    ApiConstants.Keys.CONTENT, p.getContent(),
                    "createdAt", p.getCreatedAt().toString(),
                    "displayAt", p.getDisplayAt().toString()
            )).collect(Collectors.toList());
            ctx.json(out);
        } catch (Exception e) {
            log.error("Unexpected error in list posts", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    private Long getUserIdFromAuth(Context ctx) {
        String auth = ctx.header(ApiConstants.Headers.AUTHORIZATION);
        if (auth == null || !auth.startsWith(ApiConstants.Headers.BEARER_PREFIX)) return null;
        String token = auth.substring(ApiConstants.Headers.BEARER_PREFIX.length());
        Optional<Long> opt = authService.validateAndGetUserId(token);
        return opt.orElse(null);
    }

    public void create(Context ctx) {
        try {
            Long uid = getUserIdFromAuth(ctx);
            if (uid == null) { ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.UNAUTHORIZED)); return; }

            String content;
            try {
                PostDto newPost = ctx.bodyAsClass(PostDto.class);
                content = newPost.content();
            } catch (Exception e) {
                // Fallback: try to parse as a generic map (compatibility with malformed/old clients)
                log.warn("Failed to parse body as PostDto, trying Map fallback: {}", e.getMessage());
                try {
                    Map bodyMap = ctx.bodyAsClass(Map.class);
                    content = (String) bodyMap.get(ApiConstants.Keys.CONTENT);
                } catch (Exception e2) {
                    log.error("Failed to parse request body for create post", e2);
                    throw e2;
                }
            }

            if (content == null || content.isBlank()) { ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT)); return; }
            Post p = postService.create(uid, content);
            ctx.status(201).json(Map.of("id", p.getId(), ApiConstants.Keys.CONTENT, p.getContent(), "authorId", p.getAuthorId()));
        } catch (NotFoundResponse e) {
            ctx.status(404).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.NOT_FOUND));
        } catch (Exception e) {
            log.error("Unexpected error in create post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    public void update(Context ctx) {
        try {
            Long uid = getUserIdFromAuth(ctx);
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
            ctx.json(Map.of("id", p.getId(), ApiConstants.Keys.CONTENT, p.getContent()));
        } catch (Exception e) {
            log.error("Unexpected error in update post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    public void delete(Context ctx) {
        try {
            Long uid = getUserIdFromAuth(ctx);
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

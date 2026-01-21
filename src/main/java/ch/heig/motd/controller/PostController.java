package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.dto.PostDto;
import ch.heig.motd.model.Post;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.PostService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.NotFoundResponse;
import io.javalin.openapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostController {
    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private static final String POSTS_CACHE_KEY = "all_posts";

    private final PostService postService;
    private final AuthService authService;
    private final Cache<String, List<Map<String, Object>>> postsCache;

    public PostController(PostService postService, AuthService authService) {
        this.postService = postService;
        this.authService = authService;
        this.postsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(1)
            .build();
    }

    public void requireAuth(Context ctx) {
        HandlerType method = ctx.method();
        if (!(method == HandlerType.POST || method == HandlerType.PUT || method == HandlerType.DELETE)) {
            return;
        }
        String auth = ctx.header(ApiConstants.Headers.AUTHORIZATION);
        if (auth == null || !auth.startsWith(ApiConstants.Headers.BEARER_PREFIX)) {
            ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.MISSING_TOKEN));
            return;
        }
        String token = auth.substring(ApiConstants.Headers.BEARER_PREFIX.length());
        Optional<Long> opt = authService.validateAndGetUserId(token);
        if (opt.isEmpty()) {
            ctx.status(401).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INVALID_TOKEN));
            return;
        }
        ctx.attribute("uid", opt.get());
    }

    @OpenApi(
        path = "/posts",
        methods = HttpMethod.GET,
        summary = "List all posts",
        tags = {"Posts"},
        responses = {
            @OpenApiResponse(status = "200", description = "List of posts")
        }
    )
    public void list(Context ctx) {
        try {
            List<Map<String, Object>> out = postsCache.get(POSTS_CACHE_KEY, key -> {
                List<Post> posts = postService.findAll();
                return posts.stream().map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("authorId", p.getAuthorId());
                    m.put(ApiConstants.Keys.CONTENT, p.getContent());
                    m.put("createdAt", p.getCreatedAt().toString());
                    m.put("displayAt", p.getDisplayAt().toString());
                    return m;
                }).collect(Collectors.toList());
            });
            ctx.json(out);
        } catch (Exception e) {
            log.error("Unexpected error in list posts", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    @OpenApi(
        path = "/posts",
        methods = HttpMethod.POST,
        summary = "Create a post",
        tags = {"Posts"},
        security = @OpenApiSecurity(name = "bearerAuth"),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PostDto.class)),
        responses = {
            @OpenApiResponse(status = "201", description = "Post created"),
            @OpenApiResponse(status = "401", description = "Unauthorized"),
            @OpenApiResponse(status = "400", description = "Bad request")
        }
    )
    public void create(Context ctx) {
        try {
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
                log.warn("Failed to parse body as PostDto: {}", e.getMessage());
                ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT));
                return;
            }

            if (content == null || content.isBlank()) { ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT)); return; }
            Post p = postService.create(uid, content);
            postsCache.invalidate(POSTS_CACHE_KEY);
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

    @OpenApi(
        path = "/posts/{id}",
        methods = HttpMethod.PUT,
        summary = "Update a post",
        tags = {"Posts"},
        security = @OpenApiSecurity(name = "bearerAuth"),
        pathParams = @OpenApiParam(name = "id", type = Long.class, description = "Post ID"),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PostDto.class)),
        responses = {
            @OpenApiResponse(status = "200", description = "Post updated"),
            @OpenApiResponse(status = "401", description = "Unauthorized"),
            @OpenApiResponse(status = "403", description = "Forbidden"),
            @OpenApiResponse(status = "404", description = "Not found")
        }
    )
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
            postsCache.invalidate(POSTS_CACHE_KEY);
            Map<String, Object> out = new HashMap<>();
            out.put("id", p.getId());
            out.put(ApiConstants.Keys.CONTENT, p.getContent());
            ctx.json(out);
        } catch (Exception e) {
            log.error("Unexpected error in update post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }

    @OpenApi(
        path = "/posts/{id}",
        methods = HttpMethod.DELETE,
        summary = "Delete a post",
        tags = {"Posts"},
        security = @OpenApiSecurity(name = "bearerAuth"),
        pathParams = @OpenApiParam(name = "id", type = Long.class, description = "Post ID"),
        responses = {
            @OpenApiResponse(status = "204", description = "Post deleted"),
            @OpenApiResponse(status = "401", description = "Unauthorized"),
            @OpenApiResponse(status = "403", description = "Forbidden"),
            @OpenApiResponse(status = "404", description = "Not found")
        }
    )
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
            postsCache.invalidate(POSTS_CACHE_KEY);
            ctx.status(204);
        } catch (Exception e) {
            log.error("Unexpected error in delete post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }
}

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Controller for managing posts.
 */
public class PostController {
    /**
     * Logger instance for logging.
     */
    private static final Logger log = LoggerFactory.getLogger(PostController.class);
    private static final String POSTS_CACHE_KEY = "all_posts";
    private static final DateTimeFormatter HTTP_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
            .withZone(ZoneId.of("GMT"));

    private final PostService postService;

    /**
     * Authentication service for handling authentication.
     */
    private final AuthService authService;
    private final Cache<String, List<Map<String, Object>>> postsCache;
    
    /**
     * Tracks last modification time for posts collection and individual posts.
     * Key: "all" for collection, or post ID as string for individual posts.
     */
    private final Map<String, Instant> lastModified = new ConcurrentHashMap<>();

    /**
     * Constructor.
     * @param postService post service
     * @param authService authentication service
     */
    public PostController(PostService postService, AuthService authService) {
        this.postService = postService;
        this.authService = authService;
        this.postsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(100)
            .build();
    }

    /**
     * Lists all existing posts.
     * @param ctx Javalin context
     */
    @OpenApi(
        path = "/posts",
        methods = HttpMethod.GET,
        summary = "List all posts",
        tags = {"Posts"},
        queryParams = {
            @OpenApiParam(name = "date", description = "Filter by display date (yyyy-mm-dd)", required = false)
        },
        responses = {
            @OpenApiResponse(status = "200", description = "List of posts"),
            @OpenApiResponse(status = "400", description = "Invalid date format")
        }
    )
    public void list(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            LocalDate date = null;
            if (dateParam != null) {
                try {
                    date = LocalDate.parse(dateParam);
                } catch (DateTimeParseException e) {
                    ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, "invalid.date.format"));
                    return;
                }
            }

            String ifModifiedSinceHeader = ctx.header("If-Modified-Since");
            Instant collectionLastModified = lastModified.get("all");
            
            if (ifModifiedSinceHeader != null && collectionLastModified != null) {
                try {
                    ZonedDateTime ifModifiedSince = ZonedDateTime.parse(ifModifiedSinceHeader, HTTP_DATE_FORMATTER);
                    if (!collectionLastModified.isAfter(ifModifiedSince.toInstant())) {
                        ctx.status(304);
                        return;
                    }
                } catch (DateTimeParseException e) {
                    log.warn("Invalid If-Modified-Since header: {}", ifModifiedSinceHeader);
                }
            }

            String cacheKey = date != null ? "posts_" + date : POSTS_CACHE_KEY;
            LocalDate finalDate = date;
            List<Map<String, Object>> out = postsCache.get(cacheKey, key -> {
                List<Post> posts = finalDate != null ? postService.findByDate(finalDate) : postService.findAll();
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
            
            if (collectionLastModified != null) {
                ctx.header("Last-Modified", HTTP_DATE_FORMATTER.format(collectionLastModified));
            }
            
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
            
            Instant now = Instant.now();
            lastModified.put(String.valueOf(p.getId()), now);
            lastModified.put("all", now);
            postsCache.invalidateAll();
            
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
            
            String ifUnmodifiedSinceHeader = ctx.header("If-Unmodified-Since");
            Instant postLastModified = lastModified.get(String.valueOf(id));
            
            if (ifUnmodifiedSinceHeader != null && postLastModified != null) {
                try {
                    ZonedDateTime ifUnmodifiedSince = ZonedDateTime.parse(ifUnmodifiedSinceHeader, HTTP_DATE_FORMATTER);
                    if (postLastModified.isAfter(ifUnmodifiedSince.toInstant())) {
                        ctx.status(412).json(Map.of(ApiConstants.Keys.ERROR, "precondition.failed"));
                        return;
                    }
                } catch (DateTimeParseException e) {
                    log.warn("Invalid If-Unmodified-Since header: {}", ifUnmodifiedSinceHeader);
                }
            }
            
            Map body = ctx.bodyAsClass(Map.class);
            String content = (String) body.get(ApiConstants.Keys.CONTENT);
            if (content == null || content.isBlank()) { ctx.status(400).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.EMPTY_CONTENT)); return; }
            p = postService.updateContent(id, content);
            
            Instant now = Instant.now();
            lastModified.put(String.valueOf(id), now);
            lastModified.put("all", now);
            postsCache.invalidateAll();
            
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
            
            String ifUnmodifiedSinceHeader = ctx.header("If-Unmodified-Since");
            Instant postLastModified = lastModified.get(String.valueOf(id));
            
            if (ifUnmodifiedSinceHeader != null && postLastModified != null) {
                try {
                    ZonedDateTime ifUnmodifiedSince = ZonedDateTime.parse(ifUnmodifiedSinceHeader, HTTP_DATE_FORMATTER);
                    if (postLastModified.isAfter(ifUnmodifiedSince.toInstant())) {
                        ctx.status(412).json(Map.of(ApiConstants.Keys.ERROR, "precondition.failed"));
                        return;
                    }
                } catch (DateTimeParseException e) {
                    log.warn("Invalid If-Unmodified-Since header: {}", ifUnmodifiedSinceHeader);
                }
            }
            
            postService.delete(id);
            
            Instant now = Instant.now();
            lastModified.remove(String.valueOf(id));
            lastModified.put("all", now);
            postsCache.invalidateAll();
            
            ctx.status(204);
        } catch (Exception e) {
            log.error("Unexpected error in delete post", e);
            ctx.status(500).json(Map.of(ApiConstants.Keys.ERROR, ApiConstants.Errors.INTERNAL_ERROR));
        }
    }
}

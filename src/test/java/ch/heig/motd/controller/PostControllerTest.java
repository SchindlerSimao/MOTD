package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.model.Post;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.PostService;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class PostControllerTest {
    private PostService postService;
    private AuthService authService;
    private PostController controller;
    private Context ctx;

    @BeforeEach
    public void setup() {
        postService = mock(PostService.class);
        authService = mock(AuthService.class);
        controller = new PostController(postService, authService);
        ctx = mock(Context.class);
        when(ctx.status(anyInt())).thenReturn(ctx);
    }

    @Test
    public void list_returnsPosts() {
        var post = new Post(1L, 42L, "test post", Instant.now(), LocalDate.now());
        when(postService.findAll()).thenReturn(List.of(post));

        controller.list(ctx);

        verify(ctx).json(argThat(obj -> ((List) obj).size() == 1));
    }

    @Test
    public void create_missingAuth_returns401() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(null);

        controller.create(ctx);

        verify(ctx).status(401);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.UNAUTHORIZED)));
    }

    @Test
    public void create_emptyContent_returns400() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn("Bearer token123");
        when(authService.validateAndGetUserId("token123")).thenReturn(Optional.of(42L));
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(ApiConstants.Keys.CONTENT, ""));

        controller.create(ctx);

        verify(ctx).status(400);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.EMPTY_CONTENT)));
    }

    @Test
    public void create_validRequest_returns201() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn("Bearer token123");
        when(authService.validateAndGetUserId("token123")).thenReturn(Optional.of(42L));
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(ApiConstants.Keys.CONTENT, "hello world"));
        var post = new Post(1L, 42L, "hello world", Instant.now(), LocalDate.now());
        when(postService.create(42L, "hello world")).thenReturn(post);

        controller.create(ctx);

        verify(ctx).status(201);
        verify(ctx).json(argThat(obj -> ((Map) obj).get("id").equals(1L)));
    }

    @Test
    public void update_notFound_returns404() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn("Bearer token123");
        when(authService.validateAndGetUserId("token123")).thenReturn(Optional.of(42L));
        when(ctx.pathParam("id")).thenReturn("999");
        when(postService.findById(999L)).thenReturn(Optional.empty());

        controller.update(ctx);

        verify(ctx).status(404);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.NOT_FOUND)));
    }

    @Test
    public void update_notAuthor_returns403() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn("Bearer token123");
        when(authService.validateAndGetUserId("token123")).thenReturn(Optional.of(42L));
        when(ctx.pathParam("id")).thenReturn("1");
        var post = new Post(1L, 99L, "test", Instant.now(), LocalDate.now());
        when(postService.findById(1L)).thenReturn(Optional.of(post));

        controller.update(ctx);

        verify(ctx).status(403);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.FORBIDDEN)));
    }

    @Test
    public void delete_notFound_returns404() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn("Bearer token123");
        when(authService.validateAndGetUserId("token123")).thenReturn(Optional.of(42L));
        when(ctx.pathParam("id")).thenReturn("999");
        when(postService.findById(999L)).thenReturn(Optional.empty());

        controller.delete(ctx);

        verify(ctx).status(404);
    }

    @Test
    public void delete_notAuthor_returns403() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn("Bearer token123");
        when(authService.validateAndGetUserId("token123")).thenReturn(Optional.of(42L));
        when(ctx.pathParam("id")).thenReturn("1");
        var post = new Post(1L, 99L, "test", Instant.now(), LocalDate.now());
        when(postService.findById(1L)).thenReturn(Optional.of(post));

        controller.delete(ctx);

        verify(ctx).status(403);
    }

    @Test
    public void delete_validRequest_returns204() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn("Bearer token123");
        when(authService.validateAndGetUserId("token123")).thenReturn(Optional.of(42L));
        when(ctx.pathParam("id")).thenReturn("1");
        var post = new Post(1L, 42L, "test", Instant.now(), LocalDate.now());
        when(postService.findById(1L)).thenReturn(Optional.of(post));

        controller.delete(ctx);

        verify(ctx).status(204);
        verify(postService).delete(1L);
    }
}

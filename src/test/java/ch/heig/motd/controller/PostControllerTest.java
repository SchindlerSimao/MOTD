package ch.heig.motd.controller;

import ch.heig.motd.api.ApiConstants;
import ch.heig.motd.dto.PostDto;
import ch.heig.motd.model.Post;
import ch.heig.motd.service.AuthService;
import ch.heig.motd.service.PostService;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
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
    public void create_missingAuth_returns401() {
        when(ctx.header(ApiConstants.Headers.AUTHORIZATION)).thenReturn(null);

        controller.create(ctx);

        verify(ctx).status(401);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.UNAUTHORIZED)));
    }

    @Test
    public void create_emptyContent_returns400() {
        when(ctx.attribute("uid")).thenReturn(1L);
        when(ctx.bodyAsClass(PostDto.class)).thenReturn(new PostDto(""));

        controller.create(ctx);

        verify(ctx).status(400);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.EMPTY_CONTENT)));
    }

    @Test
    public void create_userNotFound_returns404() {
        when(ctx.attribute("uid")).thenReturn(42L);
        when(ctx.bodyAsClass(PostDto.class)).thenReturn(new PostDto("hello"));
        when(postService.create(42L, "hello")).thenThrow(new NotFoundResponse("user not found"));

        controller.create(ctx);

        verify(ctx).status(404);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals(ApiConstants.Errors.NOT_FOUND)));
    }

    @Test
    public void create_validRequest_returns201() {
        when(ctx.attribute("uid")).thenReturn(3L);
        when(ctx.bodyAsClass(PostDto.class)).thenReturn(new PostDto("Salut"));
        Post created = new Post(1L, 3L, "Salut", Instant.now(), LocalDate.now());
        when(postService.create(3L, "Salut")).thenReturn(created);

        controller.create(ctx);

        verify(ctx).status(201);
        verify(ctx).json(argThat(obj -> ((Map) obj).get("id").equals(1L) && ((Map) obj).get("authorId").equals(3L)));
    }

    @Test
    public void list_returnsPosts() {
        Post p = new Post(1L, 2L, "c", Instant.now(), LocalDate.now());
        when(postService.findAll()).thenReturn(List.of(p));

        controller.list(ctx);

        verify(ctx).json(any());
    }

    @Test
    public void list_usesCache() {
        Post p = new Post(1L, 2L, "c", Instant.now(), LocalDate.now());
        when(postService.findAll()).thenReturn(List.of(p));

        controller.list(ctx);
        controller.list(ctx);

        verify(postService, times(1)).findAll();
    }

    @Test
    public void create_invalidatesCache() {
        Post p = new Post(1L, 3L, "c", Instant.now(), LocalDate.now());
        when(postService.findAll()).thenReturn(List.of(p));
        when(ctx.attribute("uid")).thenReturn(3L);
        when(ctx.bodyAsClass(PostDto.class)).thenReturn(new PostDto("new"));
        when(postService.create(3L, "new")).thenReturn(p);

        controller.list(ctx);
        controller.create(ctx);
        controller.list(ctx);

        verify(postService, times(2)).findAll();
    }

    @Test
    public void update_invalidatesCache() {
        Post p = new Post(1L, 3L, "c", Instant.now(), LocalDate.now());
        when(postService.findAll()).thenReturn(List.of(p));
        when(ctx.attribute("uid")).thenReturn(3L);
        when(ctx.pathParam("id")).thenReturn("1");
        when(postService.findById(1L)).thenReturn(Optional.of(p));
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of(ApiConstants.Keys.CONTENT, "updated"));
        when(postService.updateContent(1L, "updated")).thenReturn(p);

        controller.list(ctx);
        controller.update(ctx);
        controller.list(ctx);

        verify(postService, times(2)).findAll();
    }

    @Test
    public void delete_invalidatesCache() {
        Post p = new Post(1L, 3L, "c", Instant.now(), LocalDate.now());
        when(postService.findAll()).thenReturn(List.of(p));
        when(ctx.attribute("uid")).thenReturn(3L);
        when(ctx.pathParam("id")).thenReturn("1");
        when(postService.findById(1L)).thenReturn(Optional.of(p));

        controller.list(ctx);
        controller.delete(ctx);
        controller.list(ctx);

        verify(postService, times(2)).findAll();
    }

    @Test
    public void update_notFound_returns404() {
        when(ctx.attribute("uid")).thenReturn(1L);
        when(ctx.pathParam("id")).thenReturn("5");
        when(postService.findById(5L)).thenReturn(Optional.empty());

        controller.update(ctx);

        verify(ctx).status(404);
    }

    @Test
    public void update_notAuthor_returns403() {
        when(ctx.attribute("uid")).thenReturn(2L);
        when(ctx.pathParam("id")).thenReturn("10");
        Post p = new Post(10L, 3L, "x", Instant.now(), LocalDate.now());
        when(postService.findById(10L)).thenReturn(Optional.of(p));

        controller.update(ctx);

        verify(ctx).status(403);
    }

    @Test
    public void delete_notFound_returns404() {
        when(ctx.attribute("uid")).thenReturn(1L);
        when(ctx.pathParam("id")).thenReturn("7");
        when(postService.findById(7L)).thenReturn(Optional.empty());

        controller.delete(ctx);

        verify(ctx).status(404);
    }

    @Test
    public void delete_notAuthor_returns403() {
        when(ctx.attribute("uid")).thenReturn(2L);
        when(ctx.pathParam("id")).thenReturn("8");
        Post p = new Post(8L, 9L, "x", Instant.now(), LocalDate.now());
        when(postService.findById(8L)).thenReturn(Optional.of(p));

        controller.delete(ctx);

        verify(ctx).status(403);
    }

    @Test
    public void delete_validRequest_returns204() {
        when(ctx.attribute("uid")).thenReturn(4L);
        when(ctx.pathParam("id")).thenReturn("11");
        Post p = new Post(11L, 4L, "x", Instant.now(), LocalDate.now());
        when(postService.findById(11L)).thenReturn(Optional.of(p));

        controller.delete(ctx);

        verify(postService).delete(11L);
        verify(ctx).status(204);
    }

    @Test
    public void list_withDateFilter_returnsPosts() {
        LocalDate date = LocalDate.of(2026, 1, 22);
        Post p = new Post(1L, 2L, "c", Instant.now(), date);
        when(ctx.queryParam("date")).thenReturn("2026-01-22");
        when(postService.findByDate(date)).thenReturn(List.of(p));

        controller.list(ctx);

        verify(postService).findByDate(date);
        verify(ctx).json(any());
    }

    @Test
    public void list_withInvalidDateFormat_returns400() {
        when(ctx.queryParam("date")).thenReturn("invalid-date");

        controller.list(ctx);

        verify(ctx).status(400);
        verify(ctx).json(argThat(obj -> ((Map) obj).get(ApiConstants.Keys.ERROR).equals("invalid.date.format")));
    }

    @Test
    public void list_withDateFilter_usesCache() {
        LocalDate date = LocalDate.of(2026, 1, 22);
        Post p = new Post(1L, 2L, "c", Instant.now(), date);
        when(ctx.queryParam("date")).thenReturn("2026-01-22");
        when(postService.findByDate(date)).thenReturn(List.of(p));

        controller.list(ctx);
        controller.list(ctx);

        verify(postService, times(1)).findByDate(date);
    }
}

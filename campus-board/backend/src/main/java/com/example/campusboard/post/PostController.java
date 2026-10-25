package com.example.campusboard.post;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<PostView> list() {
        return postService.list();
    }

    @GetMapping("/{id}")
    public PostView get(@PathVariable UUID id) {
        return postService.get(id);
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PostView> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PostInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(userId, input));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public PostView update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody PostInput input) {
        return postService.update(userId, id, input);
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        postService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}

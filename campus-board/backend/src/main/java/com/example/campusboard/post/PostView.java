package com.example.campusboard.post;

import java.time.Instant;
import java.util.UUID;

public record PostView(
        UUID id,
        UUID ownerId,
        String ownerName,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt) {
    public static PostView from(Post post) {
        return new PostView(
                post.getId(),
                post.getOwner().getId(),
                post.getOwner().getName(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}

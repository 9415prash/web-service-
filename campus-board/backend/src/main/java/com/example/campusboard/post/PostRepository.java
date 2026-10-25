package com.example.campusboard.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    @EntityGraph(attributePaths = "owner")
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

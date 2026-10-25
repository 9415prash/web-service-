package com.example.campusboard.post;

import com.example.campusboard.common.ApiException;
import com.example.campusboard.user.User;
import com.example.campusboard.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PostService {
    private static final int MAX_VISIBLE_POSTS = 50;

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PostView> list() {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, MAX_VISIBLE_POSTS)).stream()
                .map(PostView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostView get(UUID postId) {
        return PostView.from(findPost(postId));
    }

    @Transactional
    public PostView create(UUID userId, PostInput input) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "Sign in with a valid, unexpired token."));
        Post post = new Post(owner, input.title().strip(), input.content().strip());
        return PostView.from(postRepository.save(post));
    }

    @Transactional
    public PostView update(UUID userId, UUID postId, PostInput input) {
        Post post = findPost(postId);
        requireOwner(post, userId);
        post.update(input.title().strip(), input.content().strip());
        return PostView.from(postRepository.save(post));
    }

    @Transactional
    public void delete(UUID userId, UUID postId) {
        Post post = findPost(postId);
        requireOwner(post, userId);
        postRepository.delete(post);
    }

    private Post findPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                        "Post was not found."));
    }

    private void requireOwner(Post post, UUID userId) {
        if (!post.getOwner().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Only the author can change this post.");
        }
    }
}

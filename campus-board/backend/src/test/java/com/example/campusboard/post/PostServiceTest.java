package com.example.campusboard.post;

import com.example.campusboard.common.ApiException;
import com.example.campusboard.user.User;
import com.example.campusboard.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, userRepository);
    }

    @Test
    void createsPostWithAuthenticatedOwnerRatherThanRequestOwner() {
        User owner = new User("Alex", "alex@example.com", "hash");
        UUID ownerId = owner.getId();
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostView created = postService.create(ownerId, new PostInput("Study group", "Meet at 3."));

        assertEquals(ownerId, created.ownerId());
        assertEquals("Study group", created.title());
    }

    @Test
    void listsAtMostTheLatestFiftyPosts() {
        when(postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 50))).thenReturn(List.of());

        assertEquals(List.of(), postService.list());

        verify(postRepository).findAllByOrderByCreatedAtDesc(eq(PageRequest.of(0, 50)));
    }

    @Test
    void rejectsUpdateByAnotherUser() {
        User owner = new User("Alex", "alex@example.com", "hash");
        Post post = new Post(owner, "Original", "Content");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        ApiException exception = assertThrows(ApiException.class,
                () -> postService.update(UUID.randomUUID(), post.getId(),
                        new PostInput("Changed", "Changed content")));

        assertEquals(403, exception.getStatus().value());
        assertEquals("FORBIDDEN", exception.getCode());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void rejectsDeleteByAnotherUser() {
        User owner = new User("Alex", "alex@example.com", "hash");
        Post post = new Post(owner, "Original", "Content");
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));

        ApiException exception = assertThrows(ApiException.class,
                () -> postService.delete(UUID.randomUUID(), post.getId()));

        assertEquals(403, exception.getStatus().value());
        verify(postRepository, never()).delete(any(Post.class));
    }
}

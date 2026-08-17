package org.example.post.service;

import com.example.common.dto.PostDto;
import org.example.post.dto.NewPostDto;
import org.example.post.dto.UpdatePostDto;
import org.example.post.entity.StatusPost;

import java.util.List;

public interface PostService {
    PostDto findPostById(Long postId, Long currentUserId);
    Long createPost(NewPostDto newPostDto, Long authorId);
    void updatePost(Long postId, UpdatePostDto updatePostDto, Long userId);
    void deletePost(Long userId, Long postId);
    List<PostDto> findPostsByAuthor(Long authorId, Long currentUserId, int page, int size);
    List<PostDto> findUserPostsByStatus(Long authorId, List<StatusPost> status, int page, int size);
    long registerView(Long postId, Long viewerId);
}
package org.example.post.service;

import org.example.post.dto.NewPostDto;
import org.example.post.dto.PostDto;
import org.example.post.dto.UpdatePostDto;
import org.example.post.entity.StatusPost;

import java.util.List;

public interface PostService {
    PostDto findPostById(Long postId);
    Long createPost(NewPostDto newPostDto, Long authorId);
    void updatePost(Long postId, UpdatePostDto updatePostDto, Long userId);
    void deletePost(Long userId, Long postId);
    List<PostDto> findPostsByAuthor(Long authorId);
    List<PostDto> findUserPostsByStatus(Long authorId, List<StatusPost> status, int page, int size);
}
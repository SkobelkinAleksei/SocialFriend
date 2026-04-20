package org.example.like.service;

import org.example.like.dto.LikePostDto;
import java.util.List;

public interface LikePostService {
    List<LikePostDto> getLikesByPostId(Long postId);
    void toggleLike(Long postId, Long userId);
    Long countActiveLikesByPostId(Long postId);
    boolean isLikedByUser(Long postId, Long userId);
}

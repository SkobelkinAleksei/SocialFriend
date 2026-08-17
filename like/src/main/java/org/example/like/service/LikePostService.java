package org.example.like.service;

import com.example.common.dto.PostLikeSummaryDto;
import org.example.like.dto.LikePostDto;
import org.example.like.dto.ToggleLikeResponseDto;

import java.util.List;

public interface LikePostService {
    List<LikePostDto> getLikesByPostId(Long postId, Long viewerId);
    ToggleLikeResponseDto toggleLike(Long postId, Long userId);
    Long countActiveLikesByPostId(Long postId, Long viewerId);
    boolean isLikedByUser(Long postId, Long userId);
    List<PostLikeSummaryDto> getSummaryByPostIds(List<Long> postIds, Long userId);
}

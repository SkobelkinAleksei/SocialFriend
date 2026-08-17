package org.example.comment.service.impl;

import com.example.common.RequestData;
import com.example.common.dto.PostDto;
import com.example.common.dto.PostCommentCountDto;
import com.example.common.dto.event.UserDto;
import com.example.common.kafka.NotificationEvent;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.comment.dto.CommentDto;
import org.example.comment.dto.CommentVoteDto;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.entity.CommentTargetType;
import org.example.comment.entity.CommentVoteEntity;
import org.example.comment.entity.CommentVoteType;
import org.example.comment.mapper.CommentMapper;
import org.example.comment.repository.CommentRepository;
import org.example.comment.repository.CommentVoteRepository;
import org.example.comment.service.PublicCommentService;
import org.example.comment.util.CommentLookupService;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.expression.AccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublicCommentServiceImpl implements PublicCommentService {

    private final CommentRepository commentRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final CommentMapper commentMapper;
    private final CommentLookupService commentLookupService;
    private final NotificationKafkaProducer notificationProducer;
    private final IHttpCore httpCore;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    @Override
    @Transactional
    public CommentDto createComment(Long authorId, Long postId, NewCommentDto newCommentDto) throws AccessException {
        PostDto postDto = commentLookupService.getPostDtoFromApi(postId, authorId);

        if (!postDto.isCommentsAllowed()) {
            log.warn("[PublicCommentServiceImpl] Попытка оставить комментарий к закрытому посту id: {}", postId);
            throw new IllegalStateException("Автор запретил комментировать этот пост.");
        }

        if (!postDto.getAuthorId().equals(authorId) && !postDto.isCanComment()) {
            log.warn("[PublicCommentServiceImpl] Блокировка: user {} → post author {}, canComment=false",
                    authorId, postDto.getAuthorId());
            throw new AccessException("Автор ограничил возможность комментирования для лиц, не входящих в список друзей.");
        }

        CommentEntity commentEntity = commentMapper.toEntity(newCommentDto);
        commentEntity.setAuthorId(authorId);
        commentEntity.setPostId(postId);
        commentEntity.setTargetType(CommentTargetType.POST);
        commentEntity.setTargetId(postId);
        commentEntity.setCommentStatus(CommentStatus.PUBLISHED);
        applyReplyMeta(commentEntity, newCommentDto, CommentTargetType.POST, postId);
        CommentEntity saved = commentRepository.save(commentEntity);

        CommentDto dto = enrichComments(List.of(commentMapper.toDto(saved)), authorId).get(0);

        String commentatorFirstName = dto.getAuthorFirstName() != null ? dto.getAuthorFirstName() : "Житель";
        String commentatorLastName = dto.getAuthorLastName() != null ? dto.getAuthorLastName() : "Района";
        sendLiveComment(
                authorId,
                commentatorFirstName,
                commentatorLastName,
                postId,
                saved.getId(),
                newCommentDto.getContent(),
                "POST",
                dto.getAuthorAvatarUrl(),
                saved.getReplyToUserId(),
                dto.getReplyToAuthorName()
        );
        notifyCommentCreated(
                postDto.getAuthorId(),
                authorId,
                commentatorFirstName,
                commentatorLastName,
                postId,
                saved.getId(),
                newCommentDto.getContent(),
                null,
                saved.getReplyToUserId()
        );
        log.info("[PublicCommentServiceImpl] Комментарий {} создан пользователем {}", saved.getId(), authorId);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> getCommentsByPostId(Long postId, int page, int size, Long viewerId) {
        assertCanViewPost(postId, viewerId);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        log.info("[PublicCommentServiceImpl] Пагинация комментариев post={}, page={}, size={}", postId, safePage, safeSize);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                org.springframework.data.domain.Sort.by("createdAt").descending()
        );

        Page<CommentEntity> entitiesPage = commentRepository.findAllByPostIdAndCommentStatus(
                postId,
                CommentStatus.PUBLISHED,
                pageable
        );

        List<CommentDto> enriched = enrichComments(entitiesPage.getContent().stream()
                .map(commentMapper::toDto)
                .toList(), viewerId);

        return new org.springframework.data.domain.PageImpl<>(
                enriched,
                pageable,
                entitiesPage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCommentsByPostId(Long postId, Long viewerId) {
        assertCanViewPost(postId, viewerId);
        return commentRepository.countByPostIdAndCommentStatus(postId, CommentStatus.PUBLISHED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostCommentCountDto> countCommentsByPostIds(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = postIds.stream().filter(java.util.Objects::nonNull).distinct().limit(100).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        for (Object[] row : commentRepository.countPublishedByPostIds(ids, CommentStatus.PUBLISHED)) {
            if (row == null || row[0] == null) {
                continue;
            }
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return ids.stream()
                .map(id -> new PostCommentCountDto(id, counts.getOrDefault(id, 0L)))
                .toList();
    }

    @Override
    @Transactional
    public CommentDto createPhotoComment(Long authorId, String kindRaw, Long targetId, NewCommentDto newCommentDto) throws AccessException {
        CommentTargetType targetType = parsePhotoTarget(kindRaw);
        var target = commentLookupService.getPhotoCommentTarget(targetType.name(), targetId, authorId);
        if (!target.isCanComment()) {
            throw new AccessException("Автор ограничил возможность комментирования.");
        }
        CommentEntity commentEntity = commentMapper.toEntity(newCommentDto);
        commentEntity.setAuthorId(authorId);
        commentEntity.setPostId(null);
        commentEntity.setTargetType(targetType);
        commentEntity.setTargetId(targetId);
        commentEntity.setCommentStatus(CommentStatus.PUBLISHED);
        applyReplyMeta(commentEntity, newCommentDto, targetType, targetId);
        CommentEntity saved = commentRepository.save(commentEntity);
        CommentDto dto = enrichComments(List.of(commentMapper.toDto(saved)), authorId).get(0);

        String commentatorFirstName = dto.getAuthorFirstName() != null ? dto.getAuthorFirstName() : "Житель";
        String commentatorLastName = dto.getAuthorLastName() != null ? dto.getAuthorLastName() : "Района";
        String contextLabel = targetType == CommentTargetType.GALLERY
                ? "GALLERY:" + targetId
                : "AVATAR:" + target.getOwnerId() + ":" + targetId;
        sendLiveComment(
                authorId,
                commentatorFirstName,
                commentatorLastName,
                targetId,
                saved.getId(),
                newCommentDto.getContent(),
                contextLabel,
                dto.getAuthorAvatarUrl(),
                saved.getReplyToUserId(),
                dto.getReplyToAuthorName()
        );
        notifyCommentCreated(
                target.getOwnerId(),
                authorId,
                commentatorFirstName,
                commentatorLastName,
                targetId,
                saved.getId(),
                newCommentDto.getContent(),
                contextLabel,
                saved.getReplyToUserId()
        );
        log.info("[PublicCommentServiceImpl] Комментарий {} к фото {}/{} создан пользователем {}",
                saved.getId(), targetType, targetId, authorId);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> getPhotoComments(String kindRaw, Long targetId, int page, int size, Long viewerId) {
        CommentTargetType targetType = parsePhotoTarget(kindRaw);
        assertCanViewPhoto(targetType.name(), targetId, viewerId);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                org.springframework.data.domain.Sort.by("createdAt").descending()
        );
        Page<CommentEntity> entitiesPage = commentRepository.findAllByTargetTypeAndTargetIdAndCommentStatus(
                targetType,
                targetId,
                CommentStatus.PUBLISHED,
                pageable
        );
        List<CommentDto> enriched = enrichComments(entitiesPage.getContent().stream()
                .map(commentMapper::toDto)
                .toList(), viewerId);
        return new org.springframework.data.domain.PageImpl<>(enriched, pageable, entitiesPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long countPhotoComments(String kindRaw, Long targetId, Long viewerId) {
        CommentTargetType targetType = parsePhotoTarget(kindRaw);
        assertCanViewPhoto(targetType.name(), targetId, viewerId);
        return commentRepository.countByTargetTypeAndTargetIdAndCommentStatus(
                targetType, targetId, CommentStatus.PUBLISHED);
    }

    @Override
    @Transactional
    public CommentVoteDto toggleVote(Long commentId, Long userId, String voteRaw) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден"));
        if (comment.getCommentStatus() != CommentStatus.PUBLISHED) {
            throw new EntityNotFoundException("Комментарий не найден");
        }
        assertCanViewCommentTarget(comment, userId);
        CommentVoteType requested = parseVote(voteRaw);
        Optional<CommentVoteEntity> existing = commentVoteRepository.findByCommentIdAndUserIdForUpdate(commentId, userId);
        String myVote = null;
        if (existing.isPresent()) {
            CommentVoteEntity vote = existing.get();
            if (vote.getVote() == requested) {
                commentVoteRepository.delete(vote);
            } else {
                vote.setVote(requested);
                commentVoteRepository.save(vote);
                myVote = requested.name();
            }
        } else {
            try {
                commentVoteRepository.saveAndFlush(CommentVoteEntity.builder()
                        .commentId(commentId)
                        .userId(userId)
                        .vote(requested)
                        .build());
                myVote = requested.name();
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                CommentVoteEntity existingVote = commentVoteRepository.findByCommentIdAndUserIdForUpdate(commentId, userId)
                        .orElseThrow(() -> e);
                if (existingVote.getVote() == requested) {
                    commentVoteRepository.delete(existingVote);
                    myVote = null;
                } else {
                    existingVote.setVote(requested);
                    commentVoteRepository.save(existingVote);
                    myVote = requested.name();
                }
            }
        }

        long likes = commentVoteRepository.countByCommentIdAndVote(commentId, CommentVoteType.LIKE);
        long dislikes = commentVoteRepository.countByCommentIdAndVote(commentId, CommentVoteType.DISLIKE);

        String contextLabel = voteContextLabel(comment);
        Long targetId = comment.getTargetId() != null ? comment.getTargetId() : comment.getPostId();
        notificationProducer.sendEvent(
                0L,
                userId,
                "",
                "",
                NotificationType.COMMENT_VOTE,
                targetId,
                commentId,
                likes + ":" + dislikes,
                contextLabel
        );

        return CommentVoteDto.builder()
                .commentId(commentId)
                .likesCount(likes)
                .dislikesCount(dislikes)
                .myVote(myVote)
                .build();
    }

    private CommentVoteType parseVote(String voteRaw) {
        if (voteRaw == null) {
            throw new IllegalArgumentException("Не указан тип голоса.");
        }
        try {
            return CommentVoteType.valueOf(voteRaw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Голос может быть только LIKE или DISLIKE.");
        }
    }

    private String voteContextLabel(CommentEntity comment) {
        CommentTargetType type = comment.getTargetType();
        if (type == CommentTargetType.GALLERY) {
            return "GALLERY:" + comment.getTargetId();
        }
        if (type == CommentTargetType.AVATAR) {
            return "AVATAR:" + comment.getTargetId();
        }
        return "POST";
    }

    private void applyReplyMeta(CommentEntity entity, NewCommentDto dto, CommentTargetType type, Long targetId) {
        if (dto == null || dto.getReplyToCommentId() == null) {
            entity.setReplyToCommentId(null);
            entity.setReplyToUserId(null);
            return;
        }
        CommentEntity parent = commentRepository.findById(dto.getReplyToCommentId()).orElse(null);
        if (parent == null || parent.getCommentStatus() != CommentStatus.PUBLISHED) {
            entity.setReplyToCommentId(null);
            entity.setReplyToUserId(null);
            return;
        }
        boolean sameTarget;
        if (type == CommentTargetType.POST) {
            sameTarget = Objects.equals(parent.getPostId(), targetId)
                    || (parent.getTargetType() == CommentTargetType.POST && Objects.equals(parent.getTargetId(), targetId));
        } else {
            sameTarget = parent.getTargetType() == type && Objects.equals(parent.getTargetId(), targetId);
        }
        if (!sameTarget) {
            entity.setReplyToCommentId(null);
            entity.setReplyToUserId(null);
            return;
        }
        entity.setReplyToCommentId(parent.getId());
        entity.setReplyToUserId(parent.getAuthorId());
    }

    private void sendLiveComment(
            Long authorId,
            String firstName,
            String lastName,
            Long targetId,
            Long commentId,
            String content,
            String contextLabel,
            String authorAvatarUrl,
            Long replyToUserId,
            String replyToAuthorName
    ) {
        notificationProducer.sendEvent(NotificationEvent.builder()
                .receiverId(0L)
                .senderId(authorId)
                .senderFirstName(firstName)
                .senderLastName(lastName)
                .type(NotificationType.NEW_COMMENT)
                .targetId(targetId)
                .commentId(commentId)
                .message(content)
                .contextLabel(contextLabel)
                .authorAvatarUrl(authorAvatarUrl)
                .replyToUserId(replyToUserId)
                .replyToAuthorName(replyToAuthorName)
                .build());
    }

    private void notifyCommentCreated(
            Long ownerId,
            Long authorId,
            String firstName,
            String lastName,
            Long targetId,
            Long commentId,
            String content,
            String photoContextLabel,
            Long replyToUserId
    ) {
        boolean tagged = replyToUserId != null && !replyToUserId.equals(authorId);
        boolean ownerTagged = tagged && replyToUserId.equals(ownerId);
        boolean photo = photoContextLabel != null && !photoContextLabel.isBlank();

        if (!ownerTagged && !ownerId.equals(authorId)) {
            notificationProducer.sendEvent(
                    ownerId, authorId, firstName, lastName,
                    NotificationType.NEW_COMMENT, targetId, commentId, content, photoContextLabel
            );
        }
        if (tagged) {
            String replyLabel = photoContextLabel;
            if (!photo && !ownerTagged) {
                replyLabel = "REPLY";
            }
            notificationProducer.sendEvent(
                    replyToUserId, authorId, firstName, lastName,
                    NotificationType.COMMENT_REPLY, targetId, commentId, content, replyLabel
            );
        }
    }

    private void assertCanViewPost(Long postId, Long viewerId) {
        if (viewerId == null) {
            throw new IllegalArgumentException("Некорректный заголовок X-User-Id");
        }
        commentLookupService.getPostDtoFromApi(postId, viewerId);
    }

    private void assertCanViewPhoto(String kind, Long targetId, Long viewerId) {
        if (viewerId == null) {
            throw new IllegalArgumentException("Некорректный заголовок X-User-Id");
        }
        commentLookupService.getPhotoCommentTarget(kind, targetId, viewerId);
    }

    private void assertCanViewCommentTarget(CommentEntity comment, Long viewerId) {
        if (comment.getTargetType() == CommentTargetType.GALLERY
                || comment.getTargetType() == CommentTargetType.AVATAR) {
            assertCanViewPhoto(comment.getTargetType().name(), comment.getTargetId(), viewerId);
            return;
        }
        Long postId = comment.getPostId() != null ? comment.getPostId() : comment.getTargetId();
        if (postId == null) {
            throw new EntityNotFoundException("Комментарий не найден");
        }
        assertCanViewPost(postId, viewerId);
    }

    private CommentTargetType parsePhotoTarget(String kindRaw) {
        if (kindRaw == null) {
            throw new IllegalArgumentException("Не указан тип фото.");
        }
        try {
            CommentTargetType type = CommentTargetType.valueOf(kindRaw.trim().toUpperCase());
            if (type == CommentTargetType.POST) {
                throw new IllegalArgumentException("Некорректный тип фото.");
            }
            return type;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Некорректный тип фото.");
        }
    }

    private List<CommentDto> enrichComments(List<CommentDto> comments, Long viewerId) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = new HashSet<>();
        List<Long> commentIds = comments.stream().map(CommentDto::getId).filter(Objects::nonNull).toList();
        for (CommentDto comment : comments) {
            if (comment.getAuthorId() != null) userIds.add(comment.getAuthorId());
            if (comment.getReplyToUserId() != null) userIds.add(comment.getReplyToUserId());
        }

        Map<Long, UserDto> usersById = fetchUsersByIds(userIds.stream().toList());
        Map<Long, long[]> voteCounts = loadVoteCounts(commentIds);
        Map<Long, CommentVoteType> myVotes = viewerId == null
                ? Collections.emptyMap()
                : commentVoteRepository.findAllByCommentIdInAndUserId(commentIds, viewerId).stream()
                .collect(Collectors.toMap(CommentVoteEntity::getCommentId, CommentVoteEntity::getVote, (a, b) -> a));

        for (CommentDto comment : comments) {
            UserDto user = usersById.get(comment.getAuthorId());
            if (user != null) {
                comment.setAuthorFirstName(user.getFirstName());
                comment.setAuthorLastName(user.getLastName());
                comment.setAuthorAvatarUrl(user.getAvatarUrl());
            }
            if (comment.getReplyToUserId() != null) {
                UserDto tagged = usersById.get(comment.getReplyToUserId());
                if (tagged != null) {
                    String name = ((tagged.getFirstName() != null ? tagged.getFirstName() : "")
                            + " " + (tagged.getLastName() != null ? tagged.getLastName() : "")).trim();
                    comment.setReplyToAuthorName(name.isEmpty() ? "Сосед" : name);
                }
            }
            long[] counts = voteCounts.getOrDefault(comment.getId(), new long[]{0L, 0L});
            comment.setLikesCount(counts[0]);
            comment.setDislikesCount(counts[1]);
            CommentVoteType mine = myVotes.get(comment.getId());
            comment.setMyVote(mine != null ? mine.name() : null);
        }
        return comments;
    }

    private Map<Long, long[]> loadVoteCounts(List<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, long[]> result = new HashMap<>();
        for (Object[] row : commentVoteRepository.countGroupedByCommentId(commentIds)) {
            Long commentId = (Long) row[0];
            CommentVoteType vote = (CommentVoteType) row[1];
            long count = row[2] instanceof Number ? ((Number) row[2]).longValue() : 0L;
            long[] bucket = result.computeIfAbsent(commentId, id -> new long[]{0L, 0L});
            if (vote == CommentVoteType.LIKE) bucket[0] = count;
            else if (vote == CommentVoteType.DISLIKE) bucket[1] = count;
        }
        return result;
    }

    private Map<Long, UserDto> fetchUsersByIds(List<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            String idsParam = authorIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            String url = userBaseUrl + "/api/v1/social/users/search/by-ids?ids=" + idsParam;
            ResponseEntity<UserDto[]> response = httpCore.get(new RequestData(url), UserDto[].class);
            if (response != null && response.getBody() != null) {
                return Arrays.stream(response.getBody())
                        .filter(u -> u.getUserId() != null)
                        .collect(Collectors.toMap(UserDto::getUserId, Function.identity(), (a, b) -> a));
            }
        } catch (Exception e) {
            log.error("[PublicCommentServiceImpl] Не удалось пакетно загрузить авторов комментариев", e);
        }
        return Collections.emptyMap();
    }
}

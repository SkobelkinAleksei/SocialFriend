package org.example.user.service;

import com.example.common.RequestData;
import com.example.common.dto.PostDto;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.restclient.config.IHttpCore;
import org.example.user.dto.*;
import org.example.user.entity.*;
import org.example.user.exception.ForbiddenException;
import org.example.user.repository.AvatarHistoryRepository;
import org.example.user.repository.GalleryPhotoRepository;
import org.example.user.repository.PhotoAlbumRepository;
import org.example.user.repository.PhotoLikeRepository;
import org.example.user.repository.UserSettingsRepository;
import org.example.user.utils.UserLookupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryService {


    private final PhotoAlbumRepository albumRepository;
    private final GalleryPhotoRepository photoRepository;
    private final PhotoLikeRepository likeRepository;
    private final AvatarHistoryRepository avatarHistoryRepository;
    private final UserSettingsRepository settingsRepository;
    private final UserLookupService userLookupService;
    private final GalleryPhotoStorageService storageService;
    private final IHttpCore httpCore;
    private final NotificationKafkaProducer notificationProducer;

    @Value("${app.services.friend-base-url:http://localhost:8082}")
    private String friendBaseUrl;
    @Value("${app.services.post-base-url:http://localhost:8083}")
    private String postBaseUrl;

    @Transactional
    public List<PhotoAlbumDto> listAlbums(Long ownerId, Long viewerId) {
        assertCanView(ownerId, viewerId);
        ensureSavedAlbum(ownerId);
        boolean owner = ownerId.equals(viewerId);
        return albumRepository.findByOwnerIdOrderBySortOrderAscCreatedAtDesc(ownerId).stream()
                .filter(album -> owner || album.getKind() != PhotoAlbumKind.SAVED)
                .map(this::toAlbumDto)
                .toList();
    }

    @Transactional
    public PhotoAlbumDto createAlbum(Long ownerId, String title) {
        String cleaned = cleanTitle(title);
        PhotoAlbumEntity album = PhotoAlbumEntity.builder()
                .ownerId(ownerId)
                .title(cleaned)
                .kind(PhotoAlbumKind.CUSTOM)
                .coverMode(AlbumCoverMode.LATEST)
                .sortOrder(nextFrontSortOrder(ownerId))
                .build();
        return toAlbumDto(albumRepository.save(album));
    }

    @Transactional
    public PhotoAlbumDto updateAlbum(Long ownerId, Long albumId, UpdateAlbumRequest request) {
        PhotoAlbumEntity album = albumRepository.findByIdAndOwnerId(albumId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Альбом не найден"));
        if (request.getTitle() != null && album.getKind() == PhotoAlbumKind.CUSTOM) {
            album.setTitle(cleanTitle(request.getTitle()));
        }
        if (request.getCoverMode() != null) {
            AlbumCoverMode mode = AlbumCoverMode.valueOf(request.getCoverMode().trim().toUpperCase(Locale.ROOT));
            album.setCoverMode(mode);
            if (mode == AlbumCoverMode.LATEST) {
                album.setCoverPhotoId(null);
            }
        }
        if (request.getCoverPhotoId() != null) {
            GalleryPhotoEntity photo = photoRepository.findByIdAndOwnerId(request.getCoverPhotoId(), ownerId)
                    .orElseThrow(() -> new EntityNotFoundException("Фото не найдено"));
            if (!photo.getAlbumId().equals(album.getId())) {
                throw new IllegalArgumentException("Обложкой может быть только фото из этой группы.");
            }
            album.setCoverMode(AlbumCoverMode.CUSTOM);
            album.setCoverPhotoId(photo.getId());
        }
        return toAlbumDto(albumRepository.save(album));
    }

    @Transactional
    public List<PhotoAlbumDto> reorderAlbums(Long ownerId, List<Long> albumIds) {
        if (albumIds == null || albumIds.isEmpty()) {
            throw new IllegalArgumentException("Не указан порядок групп.");
        }
        List<PhotoAlbumEntity> owned = albumRepository.findByOwnerIdOrderBySortOrderAscCreatedAtDesc(ownerId);
        java.util.Set<Long> ownedIds = owned.stream().map(PhotoAlbumEntity::getId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> seen = new java.util.HashSet<>();
        int index = 0;
        for (Long id : albumIds) {
            if (id == null || !ownedIds.contains(id) || !seen.add(id)) continue;
            PhotoAlbumEntity album = owned.stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
            if (album == null) continue;
            album.setSortOrder(index++);
            albumRepository.save(album);
        }
        for (PhotoAlbumEntity album : owned) {
            if (seen.contains(album.getId())) continue;
            album.setSortOrder(index++);
            albumRepository.save(album);
        }
        return listAlbums(ownerId, ownerId);
    }

    @Transactional
    public void deleteAlbum(Long ownerId, Long albumId) {
        PhotoAlbumEntity album = albumRepository.findByIdAndOwnerId(albumId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Альбом не найден"));
        if (album.getKind() == PhotoAlbumKind.SAVED) {
            throw new IllegalArgumentException("Системный альбом нельзя удалить.");
        }
        List<GalleryPhotoEntity> photos = photoRepository.findByAlbumIdOrderByCreatedAtDesc(albumId);
        List<String> likeKeys = photos.stream().map(photo -> String.valueOf(photo.getId())).toList();
        if (!likeKeys.isEmpty()) {
            likeRepository.deleteByKindAndTargetKeyIn(PhotoLikeKind.GALLERY, likeKeys);
        }
        photoRepository.deleteAll(photos);
        albumRepository.delete(album);
    }

    @Transactional
    public GalleryPhotoPageDto listPhotos(Long ownerId, Long viewerId, Long albumId, int page, int size) {
        if (!canViewPhotos(ownerId, viewerId)) {
            return GalleryPhotoPageDto.builder().hidden(true).page(page).size(size).build();
        }
        ensureSavedAlbum(ownerId);
        boolean owner = ownerId.equals(viewerId);
        if (albumId != null) {
            PhotoAlbumEntity album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new EntityNotFoundException("Альбом не найден"));
            if (!album.getOwnerId().equals(ownerId)) {
                throw new EntityNotFoundException("Альбом не найден");
            }
            if (!owner && album.getKind() == PhotoAlbumKind.SAVED) {
                return GalleryPhotoPageDto.builder().hidden(true).page(page).size(size).build();
            }
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 40);
        Page<GalleryPhotoEntity> result;
        if (albumId != null) {
            result = photoRepository.findByOwnerIdAndAlbumIdOrderByCreatedAtDesc(ownerId, albumId, PageRequest.of(safePage, safeSize));
        } else {
            result = photoRepository.findPublicTimeline(ownerId, PageRequest.of(safePage, safeSize));
        }
        List<GalleryPhotoDto> items = result.getContent().stream()
                .map(photo -> toPhotoDto(photo, viewerId))
                .toList();
        return GalleryPhotoPageDto.builder()
                .items(items)
                .page(safePage)
                .size(safeSize)
                .total(result.getTotalElements())
                .hasMore(result.hasNext())
                .hidden(false)
                .build();
    }

    @Transactional
    public GalleryPhotoDto upload(Long ownerId, MultipartFile file, Long albumId) {
        PhotoAlbumEntity album = resolveAlbum(ownerId, albumId, null);
        String url = storageService.store(file);
        GalleryPhotoEntity photo = photoRepository.save(GalleryPhotoEntity.builder()
                .ownerId(ownerId)
                .albumId(album.getId())
                .url(url)
                .sourceType(PhotoSourceType.UPLOAD)
                .sourceUrl(url)
                .build());
        return toPhotoDto(photo, ownerId);
    }

    @Transactional
    public GalleryPhotoDto saveFromSource(Long ownerId, SavePhotoRequest request) {
        if (request == null || request.getSourceUrl() == null || request.getSourceUrl().isBlank()) {
            throw new IllegalArgumentException("Не указано фото для сохранения.");
        }
        String source = GalleryPhotoStorageService.normalizePublicPath(request.getSourceUrl());
        PhotoAlbumEntity album = resolveAlbum(ownerId, request.getAlbumId(), request.getNewAlbumTitle());
        Optional<GalleryPhotoEntity> existing = photoRepository.findByOwnerIdAndUrl(ownerId, source);
        if (existing.isPresent()) {
            GalleryPhotoEntity photo = existing.get();
            photo.setAlbumId(album.getId());
            return toPhotoDto(photoRepository.save(photo), ownerId);
        }
        String stored = source.startsWith(GalleryPhotoStorageService.MEDIA_PREFIX)
                ? source
                : storageService.copyFromPublicUrl(source);
        existing = photoRepository.findByOwnerIdAndUrl(ownerId, stored);
        if (existing.isPresent()) {
            GalleryPhotoEntity photo = existing.get();
            photo.setAlbumId(album.getId());
            return toPhotoDto(photoRepository.save(photo), ownerId);
        }
        PhotoSourceType sourceType = detectSource(source);
        GalleryPhotoEntity photo = photoRepository.save(GalleryPhotoEntity.builder()
                .ownerId(ownerId)
                .albumId(album.getId())
                .url(stored)
                .sourceType(sourceType)
                .sourceUrl(source)
                .build());
        return toPhotoDto(photo, ownerId);
    }

    @Transactional
    public void deletePhoto(Long ownerId, Long photoId) {
        GalleryPhotoEntity photo = photoRepository.findByIdAndOwnerId(photoId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Фото не найдено"));
        albumRepository.findByOwnerIdOrderBySortOrderAscCreatedAtDesc(ownerId).stream()
                .filter(album -> photoId.equals(album.getCoverPhotoId()))
                .forEach(album -> {
                    album.setCoverPhotoId(null);
                    album.setCoverMode(AlbumCoverMode.LATEST);
                    albumRepository.save(album);
                });
        photoRepository.delete(photo);
    }

    @Transactional
    public void deletePhotoForModeration(Long photoId) {
        GalleryPhotoEntity photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Фото не найдено"));
        deletePhoto(photo.getOwnerId(), photoId);
    }

    @Transactional
    public void deleteAvatarForModeration(Long historyId) {
        AvatarHistoryEntity item = avatarHistoryRepository.findById(historyId)
                .orElseThrow(() -> new EntityNotFoundException("Аватарка не найдена"));
        deleteAvatar(item.getUserId(), historyId);
    }

    @Transactional
    public String setAvatarFromUrl(Long ownerId, String sourceUrl) {
        return applyAvatar(ownerId, storageService.duplicate(sourceUrl));
    }

    @Transactional
    public String setAvatarFromPhoto(Long ownerId, Long photoId) {
        GalleryPhotoEntity photo = photoRepository.findByIdAndOwnerId(photoId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Фото не найдено"));
        return applyAvatar(ownerId, storageService.duplicate(photo.getUrl()));
    }

    @Transactional
    public String uploadAvatar(Long ownerId, MultipartFile file, MultipartFile original) {
        String crop = storageService.store(file);
        String full = original != null && !original.isEmpty() ? storageService.store(original) : crop;
        return applyAvatar(ownerId, crop, full);
    }

    @Transactional
    public List<AvatarHistoryDto> listAvatars(Long ownerId, Long viewerId) {
        syncCurrentAvatar(ownerId);
        String current = userLookupService.getById(ownerId).getAvatarUrl();
        return avatarHistoryRepository.findByUserIdOrderByCreatedAtDesc(ownerId).stream()
                .map(item -> AvatarHistoryDto.builder()
                        .id(item.getId())
                        .userId(item.getUserId())
                        .url(item.getUrl())
                        .viewUrl(item.getFullUrl() != null && !item.getFullUrl().isBlank() ? item.getFullUrl() : item.getUrl())
                        .current(item.getUrl() != null && item.getUrl().equals(current))
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public String deleteAvatar(Long ownerId, Long historyId) {
        AvatarHistoryEntity item = avatarHistoryRepository.findByIdAndUserId(historyId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Аватарка не найдена"));
        UserEntity user = userLookupService.getById(ownerId);
        boolean wasCurrent = item.getUrl() != null && item.getUrl().equals(user.getAvatarUrl());
        avatarHistoryRepository.delete(item);
        if (wasCurrent) {
            String previous = avatarHistoryRepository.findByUserIdOrderByCreatedAtDesc(ownerId).stream()
                    .findFirst()
                    .map(AvatarHistoryEntity::getUrl)
                    .orElse(null);
            user.setAvatarUrl(previous);
        }
        return user.getAvatarUrl();
    }

    @Transactional
    public String restoreAvatar(Long ownerId, Long historyId) {
        AvatarHistoryEntity item = avatarHistoryRepository.findByIdAndUserId(historyId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Аватарка не найдена"));
        UserEntity user = userLookupService.getById(ownerId);
        user.setAvatarUrl(item.getUrl());
        return item.getUrl();
    }

    @Transactional
    public void updateCover(Long ownerId, UpdateCoverRequest request) {
        UserEntity user = userLookupService.getById(ownerId);
        CoverMode mode = CoverMode.COLOR;
        if (request.getMode() != null) {
            mode = CoverMode.valueOf(request.getMode().trim().toUpperCase(Locale.ROOT));
        }
        user.setCoverMode(mode);
        if (mode == CoverMode.COLOR) {
            String color = request.getColor() == null ? "#8A76B0" : request.getColor().trim();
            if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
                throw new IllegalArgumentException("Некорректный цвет обложки.");
            }
            user.setCoverColor(color.toUpperCase(Locale.ROOT));
            user.setCoverUrl(null);
        } else if (mode == CoverMode.TRANSPARENT) {
            user.setCoverUrl(null);
        } else if (mode == CoverMode.PHOTO) {
            if (request.getCoverUrl() != null && !request.getCoverUrl().isBlank()) {
                user.setCoverUrl(storageService.copyFromPublicUrl(request.getCoverUrl()));
            }
            if (user.getCoverUrl() == null || user.getCoverUrl().isBlank()) {
                throw new IllegalArgumentException("Загрузите фото для фона.");
            }
        }
    }

    @Transactional
    public String uploadCover(Long ownerId, MultipartFile file) {
        UserEntity user = userLookupService.getById(ownerId);
        String stored = storageService.store(file);
        user.setCoverMode(CoverMode.PHOTO);
        user.setCoverUrl(stored);
        return stored;
    }

    @Transactional
    public void clearCoverPhoto(Long ownerId) {
        UserEntity user = userLookupService.getById(ownerId);
        user.setCoverUrl(null);
        if (user.getCoverMode() == CoverMode.PHOTO) {
            user.setCoverMode(user.getCoverColor() != null ? CoverMode.COLOR : CoverMode.TRANSPARENT);
        }
    }

    @Transactional
    public TogglePhotoLikeResponse toggleLike(Long likerId, TogglePhotoLikeRequest request) {
        PhotoLikeKind kind = PhotoLikeKind.valueOf(request.getKind().trim().toUpperCase(Locale.ROOT));
        if (kind == PhotoLikeKind.COVER) {
            throw new IllegalArgumentException("Цвет обложки нельзя лайкнуть.");
        }
        String targetKey = request.getTargetKey() == null ? "" : GalleryPhotoStorageService.normalizePublicPath(request.getTargetKey());
        if (targetKey.isBlank()) {
            throw new IllegalArgumentException("Не указано фото.");
        }
        Long ownerId;
        String contextLabel;
        if (kind == PhotoLikeKind.GALLERY) {
            final Long photoId;
            try {
                photoId = Long.parseLong(targetKey);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Некорректное фото.");
            }
            GalleryPhotoEntity photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new EntityNotFoundException("Фото не найдено"));
            if (!canViewPhotos(photo.getOwnerId(), likerId)) {
                throw new ForbiddenException("Фотографии скрыты настройками приватности.");
            }
            ownerId = photo.getOwnerId();
            targetKey = String.valueOf(photo.getId());
            contextLabel = "GALLERY:" + photo.getId();
        } else if (kind == PhotoLikeKind.POST) {
            if (request.getPostId() == null) {
                throw new IllegalArgumentException("Не указан пост.");
            }
            PostDto post = fetchPost(request.getPostId(), likerId);
            final String postPhotoKey = targetKey;
            boolean belongs = post.getPhotos() != null && post.getPhotos().stream()
                    .anyMatch(url -> postPhotoKey.equals(GalleryPhotoStorageService.normalizePublicPath(url)));
            if (!belongs) {
                throw new IllegalArgumentException("Это фото не относится к посту.");
            }
            ownerId = post.getAuthorId();
            contextLabel = "POST:" + post.getId() + ":" + targetKey;
        } else if (kind == PhotoLikeKind.AVATAR) {
            if (targetKey.startsWith("h:")) {
                final Long historyId;
                try {
                    historyId = Long.parseLong(targetKey.substring(2));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Некорректная аватарка.");
                }
                AvatarHistoryEntity history = avatarHistoryRepository.findById(historyId)
                        .orElseThrow(() -> new EntityNotFoundException("Аватарка не найдена"));
                if (request.getOwnerId() != null && !history.getUserId().equals(request.getOwnerId())) {
                    throw new EntityNotFoundException("Аватарка не найдена");
                }
                ownerId = history.getUserId();
                targetKey = "h:" + history.getId();
                contextLabel = "AVATAR:" + ownerId + ":" + history.getId();
            } else {
                ownerId = request.getOwnerId() != null ? request.getOwnerId() : Long.parseLong(targetKey);
                userLookupService.getById(ownerId);
                targetKey = String.valueOf(ownerId);
                contextLabel = "AVATAR:" + ownerId;
            }
        } else {
            throw new IllegalArgumentException("Этот тип фото нельзя лайкнуть.");
        }

        PhotoLikeEntity like = likeRepository.findByLikerIdAndKindAndTargetKey(likerId, kind, targetKey)
                .orElse(null);
        boolean liked;
        if (like == null) {
            like = likeRepository.save(PhotoLikeEntity.builder()
                    .likerId(likerId)
                    .ownerId(ownerId)
                    .kind(kind)
                    .targetKey(targetKey)
                    .active(true)
                    .contextLabel(contextLabel)
                    .build());
            liked = true;
        } else {
            like.setActive(!like.isActive());
            like.setContextLabel(contextLabel);
            like = likeRepository.save(like);
            liked = like.isActive();
        }
        long count = likeRepository.countByKindAndTargetKeyAndActiveTrue(kind, targetKey);
        sendLikeNotification(like, liked, likerId, ownerId, contextLabel);
        return TogglePhotoLikeResponse.builder().liked(liked).likesCount(count).build();
    }

    @Transactional(readOnly = true)
    public GalleryPhotoDto getPhoto(Long photoId, Long viewerId) {
        GalleryPhotoEntity photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Фото не найдено"));
        assertCanView(photo.getOwnerId(), viewerId);
        if (!photo.getOwnerId().equals(viewerId)) {
            PhotoAlbumEntity album = albumRepository.findById(photo.getAlbumId()).orElse(null);
            if (album != null && album.getKind() == PhotoAlbumKind.SAVED) {
                throw new EntityNotFoundException("Фото не найдено");
            }
        }
        return toPhotoDto(photo, viewerId);
    }

    @Transactional(readOnly = true)
    public TogglePhotoLikeResponse likeStatus(Long viewerId, String kindRaw, String targetKeyRaw, Long ownerId, Long postId) {
        PhotoLikeKind kind = PhotoLikeKind.valueOf(kindRaw.trim().toUpperCase(Locale.ROOT));
        String targetKey = resolveStatusKey(kind, targetKeyRaw, ownerId);
        boolean liked = likeRepository.existsByLikerIdAndKindAndTargetKeyAndActiveTrue(viewerId, kind, targetKey);
        long count = likeRepository.countByKindAndTargetKeyAndActiveTrue(kind, targetKey);
        return TogglePhotoLikeResponse.builder().liked(liked).likesCount(count).build();
    }

    @Transactional(readOnly = true)
    public PhotoCommentTargetDto resolveCommentTarget(String kindRaw, Long targetId, Long viewerId) {
        if (kindRaw == null || targetId == null) {
            throw new IllegalArgumentException("Не указано фото.");
        }
        String kind = kindRaw.trim().toUpperCase(Locale.ROOT);
        Long ownerId;
        if ("GALLERY".equals(kind)) {
            GalleryPhotoEntity photo = photoRepository.findById(targetId)
                    .orElseThrow(() -> new EntityNotFoundException("Фото не найдено"));
            if (!photo.getOwnerId().equals(viewerId)) {
                PhotoAlbumEntity album = albumRepository.findById(photo.getAlbumId()).orElse(null);
                if (album != null && album.getKind() == PhotoAlbumKind.SAVED) {
                    throw new EntityNotFoundException("Фото не найдено");
                }
            }
            ownerId = photo.getOwnerId();
        } else if ("AVATAR".equals(kind)) {
            AvatarHistoryEntity history = avatarHistoryRepository.findById(targetId)
                    .orElseThrow(() -> new EntityNotFoundException("Аватарка не найдена"));
            ownerId = history.getUserId();
        } else {
            throw new IllegalArgumentException("Этот тип фото нельзя комментировать.");
        }
        boolean canView = canViewPhotos(ownerId, viewerId);
        if (!canView) {
            throw new ForbiddenException("Фотографии скрыты настройками приватности.");
        }
        boolean canComment = ownerId.equals(viewerId) || canCommentOnPhotos(ownerId, viewerId);
        return PhotoCommentTargetDto.builder()
                .ownerId(ownerId)
                .targetId(targetId)
                .kind(kind)
                .canView(true)
                .canComment(canComment)
                .build();
    }

    public boolean canViewPhotos(Long ownerId, Long viewerId) {
        if (ownerId.equals(viewerId)) return true;
        UserEntity owner = userLookupService.getById(ownerId);
        if (!owner.isActiveAccount()) {
            return false;
        }
        UserSettingsEntity settings = settingsRepository.findById(ownerId).orElse(null);
        PhotoVisibility visibility = settings == null || settings.getPhotoVisibility() == null
                ? PhotoVisibility.ALL
                : settings.getPhotoVisibility();
        if (visibility == PhotoVisibility.ALL) return true;
        if (visibility == PhotoVisibility.NONE) return false;
        return checkFriendship(viewerId, ownerId);
    }

    private boolean canCommentOnPhotos(Long ownerId, Long viewerId) {
        UserSettingsEntity settings = settingsRepository.findById(ownerId).orElse(null);
        boolean fromAll = settings == null || Boolean.TRUE.equals(settings.getAllowCommentsFromAll());
        if (fromAll) return true;
        return checkFriendship(viewerId, ownerId);
    }

    private void assertCanView(Long ownerId, Long viewerId) {
        if (!canViewPhotos(ownerId, viewerId)) {
            throw new ForbiddenException("Фотографии скрыты настройками приватности.");
        }
    }

    private int nextFrontSortOrder(Long ownerId) {
        return albumRepository.findByOwnerIdOrderBySortOrderAscCreatedAtDesc(ownerId).stream()
                .mapToInt(PhotoAlbumEntity::sortValue)
                .min()
                .orElse(0) - 1;
    }

    private PhotoAlbumEntity ensureSavedAlbum(Long ownerId) {
        return albumRepository.findByOwnerIdAndKind(ownerId, PhotoAlbumKind.SAVED)
                .orElseGet(() -> albumRepository.save(PhotoAlbumEntity.builder()
                        .ownerId(ownerId)
                        .title("Сохранённые")
                        .kind(PhotoAlbumKind.SAVED)
                        .coverMode(AlbumCoverMode.LATEST)
                        .sortOrder(0)
                        .build()));
    }

    private PhotoAlbumEntity resolveAlbum(Long ownerId, Long albumId, String newTitle) {
        if (newTitle != null && !newTitle.isBlank()) {
            return albumRepository.save(PhotoAlbumEntity.builder()
                    .ownerId(ownerId)
                    .title(cleanTitle(newTitle))
                    .kind(PhotoAlbumKind.CUSTOM)
                    .coverMode(AlbumCoverMode.LATEST)
                    .sortOrder(nextFrontSortOrder(ownerId))
                    .build());
        }
        if (albumId != null) {
            return albumRepository.findByIdAndOwnerId(albumId, ownerId)
                    .orElseThrow(() -> new EntityNotFoundException("Альбом не найден"));
        }
        return ensureSavedAlbum(ownerId);
    }

    private PhotoAlbumDto toAlbumDto(PhotoAlbumEntity album) {
        long count = photoRepository.countByAlbumId(album.getId());
        String coverUrl = null;
        if (album.getCoverMode() == AlbumCoverMode.CUSTOM && album.getCoverPhotoId() != null) {
            coverUrl = photoRepository.findById(album.getCoverPhotoId()).map(GalleryPhotoEntity::getUrl).orElse(null);
        }
        if (coverUrl == null) {
            coverUrl = photoRepository.findFirstByAlbumIdOrderByCreatedAtDesc(album.getId())
                    .map(GalleryPhotoEntity::getUrl)
                    .orElse(null);
        }
        return PhotoAlbumDto.builder()
                .id(album.getId())
                .title(album.getTitle())
                .kind(album.getKind().name())
                .coverMode(album.getCoverMode().name())
                .coverUrl(coverUrl)
                .photoCount(count)
                .sortOrder(album.sortValue())
                .build();
    }

    private GalleryPhotoDto toPhotoDto(GalleryPhotoEntity photo, Long viewerId) {
        String key = String.valueOf(photo.getId());
        String albumTitle = albumRepository.findById(photo.getAlbumId()).map(PhotoAlbumEntity::getTitle).orElse(null);
        return GalleryPhotoDto.builder()
                .id(photo.getId())
                .ownerId(photo.getOwnerId())
                .albumId(photo.getAlbumId())
                .albumTitle(albumTitle)
                .url(photo.getUrl())
                .createdAt(photo.getCreatedAt())
                .likesCount(likeRepository.countByKindAndTargetKeyAndActiveTrue(PhotoLikeKind.GALLERY, key))
                .liked(likeRepository.existsByLikerIdAndKindAndTargetKeyAndActiveTrue(viewerId, PhotoLikeKind.GALLERY, key))
                .build();
    }

    private String cleanTitle(String title) {
        String cleaned = title == null ? "" : title.replaceAll("[\\r\\n\\t]", " ").trim();
        if (cleaned.isBlank()) throw new IllegalArgumentException("Название группы не может быть пустым.");
        if (cleaned.length() > 80) cleaned = cleaned.substring(0, 80);
        return cleaned;
    }

    private PhotoSourceType detectSource(String path) {
        if (path.startsWith("/api/v1/social/posts/media/")) return PhotoSourceType.POST;
        if (path.startsWith("/api/v1/social/chats/media/")) return PhotoSourceType.CHAT;
        if (path.startsWith("/api/v1/social/events/media/")) return PhotoSourceType.EVENT;
        return PhotoSourceType.UPLOAD;
    }

    private boolean checkFriendship(Long viewerId, Long ownerId) {
        try {
            String friendUrl = friendBaseUrl + "/api/v1/social/friends/public/check?userId1="
                    + viewerId + "&userId2=" + ownerId;
            ResponseEntity<Boolean> response = httpCore.get(new RequestData(friendUrl), Boolean.class);
            return response != null && response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("[Gallery] Не удалось проверить дружбу", e);
            return false;
        }
    }

    private PostDto fetchPost(Long postId, Long viewerId) {
        String url = postBaseUrl + "/api/v1/social/posts/id/" + postId;
        ResponseEntity<PostDto> response = httpCore.get(new RequestData(url, Map.of("X-User-Id", String.valueOf(viewerId))), PostDto.class);
        if (response == null || response.getBody() == null) {
            throw new EntityNotFoundException("Пост не найден.");
        }
        return response.getBody();
    }

    private String resolveStatusKey(PhotoLikeKind kind, String targetKeyRaw, Long ownerId) {
        String targetKey = GalleryPhotoStorageService.normalizePublicPath(targetKeyRaw);
        if (kind == PhotoLikeKind.AVATAR) {
            if (targetKey.startsWith("h:")) {
                return targetKey;
            }
            return String.valueOf(ownerId != null ? ownerId : Long.parseLong(targetKey));
        }
        if (kind == PhotoLikeKind.COVER) {
            return String.valueOf(ownerId != null ? ownerId : Long.parseLong(targetKey));
        }
        return targetKey;
    }

    private void sendLikeNotification(PhotoLikeEntity like, boolean liked, Long likerId, Long ownerId, String contextLabel) {
        UserEntity liker = userLookupService.getById(likerId);
        String message = liked ? "лайкнул(а) ваше фото" : "__SYSTEM_LIKE_REMOVED__";
        Long likeId = like.getId();
        Runnable notify = () -> notificationProducer.sendEvent(
                ownerId,
                likerId,
                liker.getFirstName(),
                liker.getLastName(),
                NotificationType.PHOTO_LIKE,
                likeId,
                null,
                message,
                contextLabel
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notify.run();
                }
            });
        } else {
            notify.run();
        }
    }

    private String applyAvatar(Long ownerId, String storedUrl) {
        return applyAvatar(ownerId, storedUrl, storedUrl);
    }

    private String applyAvatar(Long ownerId, String cropUrl, String fullUrl) {
        UserEntity user = userLookupService.getById(ownerId);
        syncCurrentAvatar(user);
        if (cropUrl == null || cropUrl.isBlank()) {
            return user.getAvatarUrl();
        }
        if (!cropUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(cropUrl);
        }
        AvatarHistoryEntity existing = avatarHistoryRepository.findFirstByUserIdAndUrl(ownerId, cropUrl).orElse(null);
        if (existing == null) {
            avatarHistoryRepository.save(AvatarHistoryEntity.builder()
                    .userId(ownerId)
                    .url(cropUrl)
                    .fullUrl(fullUrl)
                    .build());
        } else if (existing.getFullUrl() == null && fullUrl != null) {
            existing.setFullUrl(fullUrl);
        }
        return cropUrl;
    }

    private void syncCurrentAvatar(Long ownerId) {
        syncCurrentAvatar(userLookupService.getById(ownerId));
    }

    private void syncCurrentAvatar(UserEntity user) {
        String current = user.getAvatarUrl();
        if (current == null || current.isBlank()) {
            return;
        }
        avatarHistoryRepository.findFirstByUserIdAndUrl(user.getId(), current)
                .orElseGet(() -> avatarHistoryRepository.save(AvatarHistoryEntity.builder()
                        .userId(user.getId())
                        .url(current)
                        .build()));
    }
}

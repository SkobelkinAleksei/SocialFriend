package org.example.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.user.dto.*;
import org.example.user.service.GalleryPhotoStorageService;
import org.example.user.service.GalleryService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/social/users")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;
    private final GalleryPhotoStorageService storageService;

    @GetMapping("/{userId}/albums")
    public ResponseEntity<List<PhotoAlbumDto>> albums(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long viewerId
    ) {
        return ResponseEntity.ok(galleryService.listAlbums(userId, viewerId));
    }

    @PostMapping("/me/albums")
    public ResponseEntity<PhotoAlbumDto> createAlbum(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody CreateAlbumRequest request
    ) {
        return ResponseEntity.ok(galleryService.createAlbum(userId, request.getTitle()));
    }

    @PutMapping("/me/albums/order")
    public ResponseEntity<List<PhotoAlbumDto>> reorderAlbums(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ReorderAlbumsRequest request
    ) {
        return ResponseEntity.ok(galleryService.reorderAlbums(userId, request.getAlbumIds()));
    }

    @PatchMapping("/me/albums/{albumId}")
    public ResponseEntity<PhotoAlbumDto> updateAlbum(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long albumId,
            @RequestBody UpdateAlbumRequest request
    ) {
        return ResponseEntity.ok(galleryService.updateAlbum(userId, albumId, request));
    }

    @DeleteMapping("/me/albums/{albumId}")
    public ResponseEntity<Void> deleteAlbum(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long albumId
    ) {
        galleryService.deleteAlbum(userId, albumId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/photos")
    public ResponseEntity<GalleryPhotoPageDto> photos(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long viewerId,
            @RequestParam(required = false) Long albumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        return ResponseEntity.ok(galleryService.listPhotos(userId, viewerId, albumId, page, size));
    }

    @PostMapping(value = "/me/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GalleryPhotoDto> upload(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long albumId
    ) {
        return ResponseEntity.ok(galleryService.upload(userId, file, albumId));
    }

    @PostMapping("/me/photos/save")
    public ResponseEntity<GalleryPhotoDto> save(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody SavePhotoRequest request
    ) {
        return ResponseEntity.ok(galleryService.saveFromSource(userId, request));
    }

    @DeleteMapping("/me/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long photoId
    ) {
        galleryService.deletePhoto(userId, photoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<Map<String, String>> setAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody SavePhotoRequest request
    ) {
        String url = request.getPhotoId() != null
                ? galleryService.setAvatarFromPhoto(userId, request.getPhotoId())
                : galleryService.setAvatarFromUrl(userId, request.getSourceUrl());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping(value = "/me/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "original", required = false) MultipartFile original
    ) {
        return ResponseEntity.ok(Map.of("url", galleryService.uploadAvatar(userId, file, original)));
    }

    @GetMapping("/{userId}/avatars")
    public ResponseEntity<List<AvatarHistoryDto>> avatars(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long viewerId
    ) {
        return ResponseEntity.ok(galleryService.listAvatars(userId, viewerId));
    }

    @PostMapping("/me/avatar/{historyId}/restore")
    public ResponseEntity<Map<String, String>> restoreAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long historyId
    ) {
        String url = galleryService.restoreAvatar(userId, historyId);
        return ResponseEntity.ok(Map.of("url", url == null ? "" : url));
    }

    @DeleteMapping("/me/avatar/{historyId}")
    public ResponseEntity<Map<String, String>> deleteAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long historyId
    ) {
        String url = galleryService.deleteAvatar(userId, historyId);
        return ResponseEntity.ok(Map.of("url", url == null ? "" : url));
    }

    @PutMapping("/me/cover")
    public ResponseEntity<Void> updateCover(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateCoverRequest request
    ) {
        galleryService.updateCover(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/me/cover/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadCover(
            @RequestHeader("X-User-Id") Long userId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(Map.of("url", galleryService.uploadCover(userId, file)));
    }

    @DeleteMapping("/me/cover")
    public ResponseEntity<Void> clearCoverPhoto(
            @RequestHeader("X-User-Id") Long userId
    ) {
        galleryService.clearCoverPhoto(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/photos/{photoId:\\d+}")
    public ResponseEntity<GalleryPhotoDto> photo(
            @PathVariable Long photoId,
            @RequestHeader("X-User-Id") Long viewerId
    ) {
        return ResponseEntity.ok(galleryService.getPhoto(photoId, viewerId));
    }

    @PostMapping("/photos/likes")
    public ResponseEntity<TogglePhotoLikeResponse> toggleLike(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody TogglePhotoLikeRequest request
    ) {
        return ResponseEntity.ok(galleryService.toggleLike(userId, request));
    }

    @GetMapping("/photos/likes")
    public ResponseEntity<TogglePhotoLikeResponse> likeStatus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String kind,
            @RequestParam String targetKey,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long postId
    ) {
        return ResponseEntity.ok(galleryService.likeStatus(userId, kind, targetKey, ownerId, postId));
    }

    @GetMapping("/media/{filename:.+}")
    public ResponseEntity<Resource> media(@PathVariable String filename) {
        Resource resource = storageService.load(filename);
        return ResponseEntity.ok()
                .contentType(storageService.mediaType(filename))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}

package org.example.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.chat.entity.ChatFileAttachment;
import org.example.chat.service.ChatPhotoStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@RequestMapping("/api/v1/social/chats")
@RestController
public class ChatPhotoController {

    private final ChatPhotoStorageService chatPhotoStorageService;

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId
    ) {
        String url = chatPhotoStorageService.storePhoto(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatFileAttachment> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(chatPhotoStorageService.storeFile(file));
    }

    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadVoice(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId
    ) {
        String url = chatPhotoStorageService.storeVoice(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/media/{filename:.+}")
    public ResponseEntity<Resource> media(@PathVariable String filename) {
        Resource resource = chatPhotoStorageService.load(filename);
        return ResponseEntity.ok()
                .contentType(chatPhotoStorageService.mediaType(filename))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}

package org.example.event.controller;

import lombok.RequiredArgsConstructor;
import org.example.event.service.EventPhotoStorageService;
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
@RequestMapping("/api/v1/social/events")
@RestController
public class EventPhotoController {

    private final EventPhotoStorageService eventPhotoStorageService;

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId
    ) {
        String url = eventPhotoStorageService.store(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/media/{filename:.+}")
    public ResponseEntity<Resource> media(@PathVariable String filename) {
        Resource resource = eventPhotoStorageService.load(filename);
        return ResponseEntity.ok()
                .contentType(eventPhotoStorageService.mediaType(filename))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}

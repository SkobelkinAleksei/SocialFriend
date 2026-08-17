package org.example.event.service;

import lombok.extern.slf4j.Slf4j;
import org.example.event.exception.EventValidationException;
import org.example.event.exception.PhotoNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class EventPhotoStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            "image/jpg",
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            MediaType.IMAGE_GIF_VALUE
    );

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpg", ".jpg",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp",
            MediaType.IMAGE_GIF_VALUE, ".gif"
    );

    private final Path root;

    public EventPhotoStorageService(@Value("${app.upload.dir:./uploads/events}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("[EventPhotoStorage] Каталог загрузок: {}", this.root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EventValidationException("Файл фотографии не передан.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new EventValidationException("Можно загрузить только JPG, PNG, WEBP или GIF.");
        }
        String filename = UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new EventValidationException("Некорректное имя файла.");
        }
        try {
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            log.error("[EventPhotoStorage] Не удалось сохранить файл: {}", e.getMessage());
            throw new EventValidationException("Не удалось сохранить фотографию.");
        }
        return "/api/v1/social/events/media/" + filename;
    }

    public Resource load(String filename) {
        Path file = resolveSafe(filename);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new PhotoNotFoundException();
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new PhotoNotFoundException();
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new PhotoNotFoundException();
        }
    }

    public MediaType mediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        return MediaType.IMAGE_JPEG;
    }

    private Path resolveSafe(String filename) {
        if (filename == null || !filename.matches("^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png|webp|gif)$")) {
            throw new EventValidationException("Некорректное имя файла.");
        }
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new EventValidationException("Некорректное имя файла.");
        }
        return target;
    }
}

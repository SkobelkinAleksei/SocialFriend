package org.example.post.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
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
public class PostPhotoStorageService {

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

    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final Path root;

    public PostPhotoStorageService(@Value("${app.upload.dir:./uploads/posts}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("[PostPhotoStorage] Каталог загрузок: {}", this.root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл фотографии не передан.");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("Фото не должно быть больше 5 МБ.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Можно загрузить только JPG, PNG, WEBP или GIF.");
        }
        String filename = UUID.randomUUID() + EXTENSIONS.getOrDefault(contentType, ".jpg");
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        try {
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            log.error("[PostPhotoStorage] Не удалось сохранить файл: {}", e.getMessage());
            throw new IllegalArgumentException("Не удалось сохранить фотографию.");
        }
        return "/api/v1/social/posts/media/" + filename;
    }

    public Resource load(String filename) {
        Path file = resolveSafe(filename);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new EntityNotFoundException("Фотография не найдена.");
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new EntityNotFoundException("Фотография не найдена.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new EntityNotFoundException("Фотография не найдена.");
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
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        return target;
    }
}

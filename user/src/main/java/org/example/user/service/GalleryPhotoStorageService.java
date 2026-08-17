package org.example.user.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class GalleryPhotoStorageService {

    public static final String MEDIA_PREFIX = "/api/v1/social/users/media/";

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
    private static final String FILE_PATTERN = "^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png|webp|gif)$";

    private final Path root;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.services.post-base-url:http://localhost:8083}")
    private String postBaseUrl;
    @Value("${app.services.chat-base-url:http://localhost:8087}")
    private String chatBaseUrl;
    @Value("${app.services.event-base-url:http://localhost:8088}")
    private String eventBaseUrl;
    @Value("${app.self-base-url:http://localhost:8081}")
    private String selfBaseUrl;

    public GalleryPhotoStorageService(@Value("${app.upload.dir:./uploads/users}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("[GalleryPhotoStorage] Каталог загрузок: {}", this.root);
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
        try {
            return storeBytes(file.getBytes(), contentType);
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось сохранить фотографию.");
        }
    }

    public String duplicate(String publicUrl) {
        String path = normalizePublicPath(publicUrl);
        if (path.startsWith(MEDIA_PREFIX)) {
            String filename = path.substring(MEDIA_PREFIX.length());
            Path existing = resolveSafe(filename);
            if (Files.exists(existing)) {
                try {
                    return storeBytes(Files.readAllBytes(existing), mediaType(filename).toString());
                } catch (IOException e) {
                    throw new IllegalArgumentException("Не удалось скопировать фотографию.");
                }
            }
        }
        return copyFromPublicUrl(publicUrl);
    }

    public String copyFromPublicUrl(String publicUrl) {
        String path = normalizePublicPath(publicUrl);
        if (path.startsWith(MEDIA_PREFIX)) {
            String filename = path.substring(MEDIA_PREFIX.length());
            Path existing = resolveSafe(filename);
            if (Files.exists(existing)) {
                return MEDIA_PREFIX + filename;
            }
        }
        String internal = toInternalUrl(path);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(internal))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().length == 0) {
                throw new IllegalArgumentException("Не удалось скопировать фотографию.");
            }
            if (response.body().length > MAX_PHOTO_BYTES) {
                throw new IllegalArgumentException("Фото не должно быть больше 5 МБ.");
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
            int cut = contentType.indexOf(';');
            if (cut > 0) contentType = contentType.substring(0, cut).trim().toLowerCase(Locale.ROOT);
            return storeBytes(response.body(), contentType);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GalleryPhotoStorage] Не удалось скачать {}: {}", internal, e.getMessage());
            throw new IllegalArgumentException("Не удалось скопировать фотографию.");
        }
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
        } catch (Exception e) {
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

    public static String normalizePublicPath(String src) {
        if (src == null) return "";
        String value = src.trim();
        value = value.replaceFirst("^https?://[^/]+", "");
        int q = value.indexOf('?');
        if (q >= 0) value = value.substring(0, q);
        return value;
    }

    private String storeBytes(byte[] bytes, String contentType) {
        String type = contentType == null ? "image/jpeg" : contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(type)) {
            type = MediaType.IMAGE_JPEG_VALUE;
        }
        String filename = UUID.randomUUID() + EXTENSIONS.getOrDefault(type, ".jpg");
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            Files.copy(in, target);
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось сохранить фотографию.");
        }
        return MEDIA_PREFIX + filename;
    }

    private String toInternalUrl(String path) {
        if (path.startsWith("/api/v1/social/posts/media/")) {
            return postBaseUrl + path;
        }
        if (path.startsWith("/api/v1/social/chats/media/")) {
            return chatBaseUrl + path;
        }
        if (path.startsWith("/api/v1/social/events/media/")) {
            return eventBaseUrl + path;
        }
        if (path.startsWith(MEDIA_PREFIX)) {
            return selfBaseUrl + path;
        }
        throw new IllegalArgumentException("Это фото нельзя сохранить в альбом.");
    }

    private Path resolveSafe(String filename) {
        if (filename == null || !filename.matches(FILE_PATTERN)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        return target;
    }
}

package org.example.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.example.chat.entity.ChatFileAttachment;
import org.example.chat.exception.ChatPhotoNotFoundException;
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
public class ChatPhotoStorageService {

    public enum Kind { PHOTO, FILE, VOICE }

    private static final Set<String> PHOTO_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            "image/jpg",
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            MediaType.IMAGE_GIF_VALUE
    );

    private static final Set<String> VOICE_TYPES = Set.of(
            "audio/webm",
            "audio/ogg",
            "audio/mpeg",
            "audio/mp4",
            "audio/wav",
            "audio/x-wav",
            "audio/aac",
            "video/webm"
    );

    private static final Set<String> BLOCKED_FILE_EXT = Set.of(
            "exe", "bat", "cmd", "com", "scr", "dll", "msi", "js", "vbs", "ps1", "sh", "jar"
    );

    private static final Map<String, String> PHOTO_EXT = Map.of(
            "image/jpg", ".jpg",
            MediaType.IMAGE_JPEG_VALUE, ".jpg",
            MediaType.IMAGE_PNG_VALUE, ".png",
            "image/webp", ".webp",
            MediaType.IMAGE_GIF_VALUE, ".gif"
    );

    private static final Map<String, String> VOICE_EXT = Map.of(
            "audio/webm", ".webm",
            "video/webm", ".webm",
            "audio/ogg", ".ogg",
            "audio/mpeg", ".mp3",
            "audio/mp4", ".m4a",
            "audio/wav", ".wav",
            "audio/x-wav", ".wav",
            "audio/aac", ".aac"
    );

    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final long MAX_VOICE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    private final Path root;

    public ChatPhotoStorageService(@Value("${app.upload.dir:./uploads/chats}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("[ChatPhotoStorage] Каталог загрузок: {}", this.root);
    }

    public String storePhoto(MultipartFile file) {
        return store(file, Kind.PHOTO).getUrl();
    }

    public String storeVoice(MultipartFile file) {
        return store(file, Kind.VOICE).getUrl();
    }

    public ChatFileAttachment storeFile(MultipartFile file) {
        return store(file, Kind.FILE);
    }

    public ChatFileAttachment store(MultipartFile file, Kind kind) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не передан.");
        }
        long size = file.getSize();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String originalName = safeOriginalName(file.getOriginalFilename());
        String ext;

        if (kind == Kind.PHOTO) {
            if (size > MAX_PHOTO_BYTES) {
                throw new IllegalArgumentException("Фото не должно быть больше 5 МБ.");
            }
            if (!PHOTO_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("Можно загрузить только JPG, PNG, WEBP или GIF.");
            }
            ext = PHOTO_EXT.getOrDefault(contentType, ".jpg");
        } else if (kind == Kind.VOICE) {
            if (size > MAX_VOICE_BYTES) {
                throw new IllegalArgumentException("Голосовое сообщение слишком большое.");
            }
            if (!VOICE_TYPES.contains(contentType) && !originalName.matches("(?i).+\\.(webm|ogg|mp3|m4a|wav|aac)$")) {
                throw new IllegalArgumentException("Неподдерживаемый формат голосового сообщения.");
            }
            ext = VOICE_EXT.getOrDefault(contentType, extensionOf(originalName, ".webm"));
        } else {
            if (size > MAX_FILE_BYTES) {
                throw new IllegalArgumentException("Файл не должен быть больше 20 МБ.");
            }
            ext = extensionOf(originalName, ".bin");
            String extNoDot = ext.startsWith(".") ? ext.substring(1).toLowerCase(Locale.ROOT) : ext.toLowerCase(Locale.ROOT);
            if (extNoDot.isBlank()) {
                extNoDot = "bin";
            }
            if (BLOCKED_FILE_EXT.contains(extNoDot) || !extNoDot.matches("[a-z0-9]{1,8}")) {
                throw new IllegalArgumentException("Этот тип файла нельзя отправить.");
            }
            ext = "." + extNoDot;
        }

        String filename = UUID.randomUUID() + ext.toLowerCase(Locale.ROOT);
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        try {
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            log.error("[ChatPhotoStorage] Не удалось сохранить файл: {}", e.getMessage());
            throw new IllegalArgumentException("Не удалось сохранить файл.");
        }
        return ChatFileAttachment.builder()
                .url("/api/v1/social/chats/media/" + filename)
                .name(originalName.isBlank() ? filename : originalName)
                .size(size)
                .mimeType(contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
                .build();
    }

    public Resource load(String filename) {
        Path file = resolveSafe(filename);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new ChatPhotoNotFoundException();
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ChatPhotoNotFoundException();
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ChatPhotoNotFoundException();
        }
    }

    public MediaType mediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webm")) return MediaType.parseMediaType("audio/webm");
        if (lower.endsWith(".ogg")) return MediaType.parseMediaType("audio/ogg");
        if (lower.endsWith(".mp3")) return MediaType.parseMediaType("audio/mpeg");
        if (lower.endsWith(".m4a")) return MediaType.parseMediaType("audio/mp4");
        if (lower.endsWith(".wav")) return MediaType.parseMediaType("audio/wav");
        if (lower.endsWith(".aac")) return MediaType.parseMediaType("audio/aac");
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".txt")) return MediaType.TEXT_PLAIN;
        if (lower.endsWith(".zip")) return MediaType.parseMediaType("application/zip");
        if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
        if (lower.endsWith(".mp4")) return MediaType.parseMediaType("video/mp4");
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private Path resolveSafe(String filename) {
        if (filename == null || !filename.matches("^[a-fA-F0-9\\-]{36}\\.[a-zA-Z0-9]{2,8}$")) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (BLOCKED_FILE_EXT.contains(ext)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Некорректное имя файла.");
        }
        return target;
    }

    private static String safeOriginalName(String raw) {
        if (raw == null || raw.isBlank()) return "file";
        String name = raw.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        name = name.replaceAll("[\\r\\n\\t]", " ").trim();
        if (name.length() > 180) {
            name = name.substring(name.length() - 180);
        }
        return name.isBlank() ? "file" : name;
    }

    private static String extensionOf(String name, String fallback) {
        if (name == null) return fallback;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return fallback;
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }
}

package org.example.user.service.admin;

import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.dto.admin.AdminReportDto;
import org.example.user.dto.admin.AdminReportPageDto;
import org.example.user.entity.AccountStatus;
import org.example.user.entity.PlatformRole;
import org.example.user.entity.UserEntity;
import org.example.user.entity.admin.AdminReportCategory;
import org.example.user.entity.admin.AdminReportEntity;
import org.example.user.entity.admin.AdminReportReason;
import org.example.user.entity.admin.AdminReportStatus;
import org.example.user.repository.UserRepository;
import org.example.user.repository.admin.AdminReportRepository;
import org.example.user.dto.report.CreateReportRequest;
import org.example.user.service.GalleryService;
import org.example.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final Set<AdminReportStatus> UPHELD = Set.of(
            AdminReportStatus.UPHELD_DELETED,
            AdminReportStatus.UPHELD_BANNED
    );

    private final AdminReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AdminContentClient contentClient;
    private final UserService userService;
    private final GalleryService galleryService;
    private final NotificationKafkaProducer notificationProducer;

    @Transactional(readOnly = true)
    public AdminReportPageDto list(String category, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<AdminReportEntity> result;
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            result = reportRepository.findAllByStatusOrderByCreatedAtDesc(AdminReportStatus.OPEN, pageable);
        } else {
            AdminReportCategory parsed = AdminReportCategory.valueOf(category.trim().toUpperCase());
            result = reportRepository.findAllByStatusAndCategoryOrderByCreatedAtDesc(
                    AdminReportStatus.OPEN, parsed, pageable);
        }
        long openCount = reportRepository.countByStatus(AdminReportStatus.OPEN);
        long upheldCount = reportRepository.countByStatus(AdminReportStatus.UPHELD_DELETED)
                + reportRepository.countByStatus(AdminReportStatus.UPHELD_BANNED);
        return AdminReportPageDto.builder()
                .items(result.getContent().stream().map(this::toDto).toList())
                .total(result.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .openCount(openCount)
                .upheldCount(upheldCount)
                .warnedCount(countWarned())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminReportDto get(Long id) {
        return toDto(require(id));
    }

    @Transactional
    public AdminReportDto create(Long reporterId, CreateReportRequest request) {
        if (reporterId == null || reporterId <= 0) {
            throw new IllegalArgumentException("Не удалось определить отправителя");
        }
        if (reporterId.equals(request.getAccusedId())) {
            throw new IllegalArgumentException("Нельзя отправить жалобу на свой контент");
        }
        AdminReportCategory category;
        AdminReportReason reason;
        try {
            category = AdminReportCategory.valueOf(request.getCategory().trim().toUpperCase());
            reason = AdminReportReason.valueOf(request.getReason().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестная категория или причина жалобы");
        }
        String details = request.getDetails() == null ? "" : request.getDetails().trim();
        if (reason == AdminReportReason.OTHER && details.isBlank()) {
            throw new IllegalArgumentException("Для «Другое» коротко напишите, в чём дело");
        }
        if (!userRepository.existsById(request.getAccusedId())) {
            throw new EntityNotFoundException("Пользователь не найден");
        }
        if (reportRepository.existsByReporterIdAndCategoryAndTargetIdAndStatus(
                reporterId, category, request.getTargetId(), AdminReportStatus.OPEN)) {
            throw new IllegalStateException("Вы уже отправили жалобу на это");
        }
        String snapshot = trimTo(request.getSnapshotText(), 2000);
        String note = trimTo(details, 500);
        AdminReportEntity saved = reportRepository.save(AdminReportEntity.builder()
                .reporterId(reporterId)
                .accusedId(request.getAccusedId())
                .category(category)
                .reason(reason)
                .targetId(request.getTargetId())
                .roomId(request.getRoomId())
                .targetTitle(trimTo(request.getTargetTitle(), 250))
                .snapshotText(snapshot)
                .details(note)
                .status(AdminReportStatus.OPEN)
                .build());
        return toDto(saved);
    }

    @Transactional
    public AdminReportDto dismiss(Long reportId, Long reviewerId) {
        AdminReportEntity report = requireOpen(reportId);
        report.setStatus(AdminReportStatus.DISMISSED);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewerId(reviewerId);
        return toDto(reportRepository.save(report));
    }

    @Transactional
    public AdminReportDto deleteTarget(Long reportId, Long reviewerId) {
        AdminReportEntity report = requireOpen(reportId);
        long upheld = reportRepository.countByAccusedIdAndStatusIn(report.getAccusedId(), UPHELD);
        UserEntity accused = userRepository.findById(report.getAccusedId()).orElse(null);
        if (accused != null && accused.effectiveAccountStatus() != AccountStatus.BANNED && upheld >= 1) {
            throw new IllegalStateException("Повторная верная жалоба: нужен бан, а не ещё одно удаление");
        }
        deleteReportedContent(report, reviewerId);
        report.setStatus(AdminReportStatus.UPHELD_DELETED);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewerId(reviewerId);
        AdminReportEntity saved = reportRepository.save(report);
        sendWarning(saved);
        return toDto(saved);
    }

    @Transactional
    public AdminReportDto banFromReport(Long reportId, Long reviewerId) {
        AdminReportEntity report = requireOpen(reportId);
        try {
            deleteReportedContent(report, reviewerId);
        } catch (Exception e) {
            log.warn("[Admin] Контент жалобы {} не удалился, бан всё равно ставим: {}", reportId, e.getMessage());
        }
        report.setStatus(AdminReportStatus.UPHELD_BANNED);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewerId(reviewerId);
        AdminReportEntity saved = reportRepository.save(report);
        userService.banUser(report.getAccusedId());
        return toDto(saved);
    }

    private void deleteReportedContent(AdminReportEntity report, Long adminId) {
        switch (report.getCategory()) {
            case POST -> contentClient.hidePost(adminId, report.getTargetId());
            case COMMENT -> contentClient.hideComment(adminId, report.getTargetId());
            case PHOTO -> hideReportedPhoto(report);
            case EVENT -> contentClient.cancelEvent(adminId, report.getTargetId());
            case MESSAGE -> contentClient.deleteMessage(adminId, report.getTargetId());
            case CHAT -> throw new IllegalStateException("Для жалобы на чат удалите конкретные сообщения или забаньте автора");
        }
    }

    private void sendWarning(AdminReportEntity report) {
        String what = switch (report.getCategory()) {
            case POST -> "пост";
            case COMMENT -> "комментарий";
            case PHOTO -> "фото";
            case EVENT -> "событие";
            case MESSAGE -> "сообщение";
            case CHAT -> "чат";
        };
        String message = "Ваш " + what + " удалён за нарушение правил. Если следующая жалоба будет признана верной, аккаунт забанят на районе.";
        notificationProducer.sendEvent(
                report.getAccusedId(),
                0L,
                "Система",
                "MyRaion",
                NotificationType.ADMIN_CONTENT_REMOVED,
                report.getTargetId(),
                null,
                message
        );
    }

    private void hideReportedPhoto(AdminReportEntity report) {
        if (report.getTargetTitle() != null && "AVATAR".equalsIgnoreCase(report.getTargetTitle().trim())) {
            galleryService.deleteAvatarForModeration(report.getTargetId());
            return;
        }
        galleryService.deletePhotoForModeration(report.getTargetId());
    }

    private static String trimTo(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private AdminReportEntity require(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Жалоба не найдена"));
    }

    private AdminReportEntity requireOpen(Long id) {
        AdminReportEntity report = require(id);
        if (report.getStatus() != AdminReportStatus.OPEN) {
            throw new IllegalStateException("Жалоба уже разобрана");
        }
        return report;
    }

    private long countWarned() {
        List<Object[]> rows = reportRepository.countGroupedByAccusedAndStatusIn(UPHELD);
        long warned = 0;
        for (Object[] row : rows) {
            if (row[1] instanceof Number n && n.longValue() == 1 && row[0] instanceof Number id) {
                UserEntity user = userRepository.findById(id.longValue()).orElse(null);
                if (user != null
                        && user.effectivePlatformRole() != PlatformRole.ADMIN
                        && user.effectiveAccountStatus() == AccountStatus.ACTIVE) {
                    warned++;
                }
            }
        }
        return warned;
    }

    private AdminReportDto toDto(AdminReportEntity report) {
        UserEntity reporter = userRepository.findById(report.getReporterId()).orElse(null);
        UserEntity accused = userRepository.findById(report.getAccusedId()).orElse(null);
        long total = reportRepository.countByAccusedId(report.getAccusedId());
        long upheld = reportRepository.countByAccusedIdAndStatusIn(report.getAccusedId(), UPHELD);
        return AdminReportDto.builder()
                .id(report.getId())
                .createdAt(report.getCreatedAt())
                .category(report.getCategory().name())
                .reason(report.getReason().name())
                .status(report.getStatus().name())
                .reporterId(report.getReporterId())
                .reporterName(displayName(reporter))
                .accusedId(report.getAccusedId())
                .accusedName(displayName(accused))
                .accusedStatus(accused == null ? "ACTIVE" : accused.effectiveAccountStatus().name())
                .accusedReportsTotal(total)
                .accusedReportsUpheld(upheld)
                .targetId(report.getTargetId())
                .roomId(report.getRoomId())
                .targetTitle(report.getTargetTitle())
                .snapshotText(report.getSnapshotText())
                .details(report.getDetails())
                .build();
    }

    private static String displayName(UserEntity user) {
        if (user == null) {
            return "Пользователь";
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}

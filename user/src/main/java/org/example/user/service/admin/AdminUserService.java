package org.example.user.service.admin;

import lombok.RequiredArgsConstructor;
import org.example.user.dto.admin.AdminPersonDto;
import org.example.user.dto.admin.AdminPersonPageDto;
import org.example.user.entity.UserEntity;
import org.example.user.entity.admin.AdminReportEntity;
import org.example.user.entity.admin.AdminReportStatus;
import org.example.user.exception.ForbiddenException;
import org.example.user.repository.UserRepository;
import org.example.user.repository.admin.AdminReportRepository;
import org.example.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Set<AdminReportStatus> UPHELD = Set.of(
            AdminReportStatus.UPHELD_DELETED,
            AdminReportStatus.UPHELD_BANNED
    );

    private final UserRepository userRepository;
    private final AdminReportRepository reportRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public AdminPersonPageDto search(String query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        String q = query == null ? "" : query.trim();
        Page<UserEntity> result = userRepository.searchForAdmin(q, PageRequest.of(safePage, safeSize));
        List<Long> ids = result.getContent().stream().map(UserEntity::getId).toList();
        Map<Long, Long> totals = toCountMap(ids.isEmpty()
                ? List.of()
                : reportRepository.countTotalByAccusedIds(ids));
        Map<Long, Long> upheld = toCountMap(ids.isEmpty()
                ? List.of()
                : reportRepository.countByAccusedIdsAndStatusIn(ids, UPHELD));
        List<AdminPersonDto> items = result.getContent().stream()
                .map(user -> toDto(user, totals.getOrDefault(user.getId(), 0L), upheld.getOrDefault(user.getId(), 0L)))
                .toList();
        return AdminPersonPageDto.builder()
                .items(items)
                .total(result.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminPersonDto get(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Пользователь не найден"));
        if (user.isPlatformAdmin()) {
            throw new ForbiddenException("Администратор не в этом списке");
        }
        long total = reportRepository.countByAccusedId(userId);
        long upheld = reportRepository.countByAccusedIdAndStatusIn(userId, UPHELD);
        return toDto(user, total, upheld);
    }

    @Transactional(readOnly = true)
    public List<AdminReportEntity> reportsForUser(Long userId) {
        return reportRepository.findAllByAccusedIdOrderByCreatedAtDesc(userId);
    }

    public void ban(Long userId) {
        userService.banUser(userId);
    }

    public void unban(Long userId) {
        userService.unbanUser(userId);
    }

    private static AdminPersonDto toDto(UserEntity user, long total, long upheld) {
        return AdminPersonDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .accountStatus(user.effectiveAccountStatus().name())
                .platformRole(user.effectivePlatformRole().name())
                .reportsTotal(total)
                .reportsUpheld(upheld)
                .build();
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] instanceof Number id && row[1] instanceof Number count) {
                map.put(id.longValue(), count.longValue());
            }
        }
        return map;
    }
}

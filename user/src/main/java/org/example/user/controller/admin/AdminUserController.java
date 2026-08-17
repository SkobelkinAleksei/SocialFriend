package org.example.user.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.user.dto.admin.AdminPersonDto;
import org.example.user.dto.admin.AdminPersonPageDto;
import org.example.user.dto.admin.AdminReportDto;
import org.example.user.service.admin.AdminAccess;
import org.example.user.service.admin.AdminReportService;
import org.example.user.service.admin.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/social/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminAccess adminAccess;
    private final AdminUserService adminUserService;
    private final AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<AdminPersonPageDto> search(
            @RequestHeader("X-Platform-Role") String role,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminUserService.search(q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminPersonDto> get(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminUserService.get(id));
    }

    @GetMapping("/{id}/reports")
    public ResponseEntity<List<AdminReportDto>> reports(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminUserService.get(id);
        return ResponseEntity.ok(adminUserService.reportsForUser(id).stream()
                .map(report -> adminReportService.get(report.getId()))
                .toList());
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<Void> ban(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminUserService.ban(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<Void> unban(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminUserService.unban(id);
        return ResponseEntity.noContent().build();
    }
}

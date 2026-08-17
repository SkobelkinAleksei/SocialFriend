package org.example.user.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.user.dto.admin.AdminReportDto;
import org.example.user.dto.admin.AdminReportPageDto;
import org.example.user.service.admin.AdminAccess;
import org.example.user.service.admin.AdminReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminAccess adminAccess;
    private final AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<AdminReportPageDto> list(
            @RequestHeader("X-Platform-Role") String role,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminReportService.list(category, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminReportDto> get(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminReportService.get(id));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<AdminReportDto> dismiss(
            @RequestHeader("X-Platform-Role") String role,
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminReportService.dismiss(id, adminId));
    }

    @PostMapping("/{id}/delete-target")
    public ResponseEntity<AdminReportDto> deleteTarget(
            @RequestHeader("X-Platform-Role") String role,
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminReportService.deleteTarget(id, adminId));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<AdminReportDto> ban(
            @RequestHeader("X-Platform-Role") String role,
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminReportService.banFromReport(id, adminId));
    }
}

package org.example.user.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.user.dto.admin.AdminPersonDto;
import org.example.user.dto.admin.CreateAdminRequest;
import org.example.user.service.admin.AdminAccess;
import org.example.user.service.admin.AdminStaffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/social/admin/staff")
@RequiredArgsConstructor
public class AdminStaffController {

    private final AdminAccess adminAccess;
    private final AdminStaffService adminStaffService;

    @GetMapping
    public ResponseEntity<List<AdminPersonDto>> list(@RequestHeader("X-Platform-Role") String role) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminStaffService.list());
    }

    @PostMapping
    public ResponseEntity<AdminPersonDto> createOrGrant(
            @RequestHeader("X-Platform-Role") String role,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody CreateAdminRequest request
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminStaffService.createOrGrant(request, adminId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @RequestHeader("X-Platform-Role") String role,
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminStaffService.revoke(id, adminId);
        return ResponseEntity.noContent().build();
    }
}

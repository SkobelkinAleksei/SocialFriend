package org.example.post.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.post.dto.admin.AdminPostDto;
import org.example.post.service.admin.AdminAccess;
import org.example.post.service.admin.AdminPostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminAccess adminAccess;
    private final AdminPostService adminPostService;

    @GetMapping("/{id}")
    public ResponseEntity<AdminPostDto> preview(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminPostService.preview(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hide(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminPostService.hideForModeration(id);
        return ResponseEntity.noContent().build();
    }
}

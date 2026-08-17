package org.example.comment.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.comment.dto.admin.AdminCommentDto;
import org.example.comment.service.admin.AdminAccess;
import org.example.comment.service.admin.AdminCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminAccess adminAccess;
    private final AdminCommentService adminCommentService;

    @GetMapping("/{id}")
    public ResponseEntity<AdminCommentDto> preview(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminCommentService.preview(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hide(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminCommentService.hideForModeration(id);
        return ResponseEntity.noContent().build();
    }
}

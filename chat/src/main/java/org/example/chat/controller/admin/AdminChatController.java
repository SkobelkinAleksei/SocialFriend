package org.example.chat.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.chat.entity.admin.AdminChatAroundDto;
import org.example.chat.service.admin.AdminAccess;
import org.example.chat.service.admin.AdminChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/admin/chats")
@RequiredArgsConstructor
public class AdminChatController {

    private final AdminAccess adminAccess;
    private final AdminChatService adminChatService;

    @GetMapping("/messages/{id}")
    public ResponseEntity<AdminChatAroundDto> around(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminChatService.around(id));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminChatService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
}

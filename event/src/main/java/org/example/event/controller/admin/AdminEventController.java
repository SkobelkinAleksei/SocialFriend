package org.example.event.controller.admin;

import com.example.common.dto.event.EventDto;
import lombok.RequiredArgsConstructor;
import org.example.event.service.admin.AdminAccess;
import org.example.event.service.admin.AdminEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminAccess adminAccess;
    private final AdminEventService adminEventService;

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> preview(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        return ResponseEntity.ok(adminEventService.preview(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(
            @RequestHeader("X-Platform-Role") String role,
            @PathVariable Long id
    ) {
        adminAccess.requireAdmin(role);
        adminEventService.cancelAsModerator(id);
        return ResponseEntity.noContent().build();
    }
}

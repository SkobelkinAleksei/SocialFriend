package org.example.event.service.admin;

import org.example.event.exception.EventAccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AdminAccess {

    public void requireAdmin(String platformRole) {
        if (platformRole == null || !"ADMIN".equalsIgnoreCase(platformRole.trim())) {
            throw new EventAccessDeniedException("Нет прав модератора");
        }
    }
}

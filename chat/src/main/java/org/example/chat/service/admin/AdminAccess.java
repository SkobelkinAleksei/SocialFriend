package org.example.chat.service.admin;

import org.springframework.stereotype.Component;

@Component
public class AdminAccess {

    public void requireAdmin(String platformRole) {
        if (platformRole == null || !"ADMIN".equalsIgnoreCase(platformRole.trim())) {
            throw new SecurityException("Нет прав модератора");
        }
    }
}

package org.example.user.service.admin;

import org.example.user.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class AdminAccess {

    public void requireAdmin(String platformRole) {
        if (platformRole == null || !"ADMIN".equalsIgnoreCase(platformRole.trim())) {
            throw new ForbiddenException("Нет прав модератора");
        }
    }
}

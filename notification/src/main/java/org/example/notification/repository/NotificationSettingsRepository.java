package org.example.notification.repository;

import org.example.notification.entity.NotificationSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettingsEntity, Long> {
}
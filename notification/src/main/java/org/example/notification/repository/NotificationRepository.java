package org.example.notification.repository;

import org.example.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByReceiverId(Long receiverId, Pageable pageable);
    List<NotificationEntity> findAllByReceiverIdAndIsReadFalse(Long receiverId);
}
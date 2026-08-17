package org.example.notification.repository;

import com.example.common.kafka.NotificationType;
import org.example.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findAllByReceiverId(Long receiverId, Pageable pageable);

    Page<NotificationEntity> findAllByReceiverIdAndTypeNot(Long receiverId, NotificationType type, Pageable pageable);

    List<NotificationEntity> findAllByReceiverIdAndReadFalse(Long receiverId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.receiverId = :userId AND n.read = false")
    int markAllUnreadAsRead(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.id IN :ids AND n.receiverId = :userId")
    void markIdsAsRead(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    long countByReceiverIdAndReadFalseAndTypeNot(Long receiverId, NotificationType type);

    // Ищет существующее непрочитанное уведомление конкретного типа по отправителю, получателю и посту
    Optional<NotificationEntity> findByReceiverIdAndSenderIdAndTypeAndTargetIdAndReadFalse(
            Long receiverId, Long senderId, com.example.common.kafka.NotificationType type, Long targetId
    );

    boolean existsByReceiverIdAndTypeAndTargetId(Long receiverId, NotificationType type, Long targetId);

    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM NotificationEntity n
            WHERE n.receiverId = :receiverId
              AND n.senderId = :senderId
              AND n.type = :type
              AND n.targetId = :targetId
            """)
    int deleteByReceiverSenderTypeTarget(
            @Param("receiverId") Long receiverId,
            @Param("senderId") Long senderId,
            @Param("type") NotificationType type,
            @Param("targetId") Long targetId
    );
}

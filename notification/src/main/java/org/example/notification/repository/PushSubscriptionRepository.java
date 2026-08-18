package org.example.notification.repository;

import org.example.notification.entity.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, Long> {
    List<PushSubscriptionEntity> findAllByUserId(Long userId);

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    void deleteByUserIdAndEndpoint(Long userId, String endpoint);

    void deleteByEndpoint(String endpoint);
}

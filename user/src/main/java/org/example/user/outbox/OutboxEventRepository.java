package org.example.user.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Modifying
    @Query("""
            update OutboxEvent e
            set e.status = :pending,
                e.lockedAt = null,
                e.nextAttemptAt = :now
            where e.status = :sending
              and e.lockedAt < :staleBefore
            """)
    int recoverStuckSending(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("now") LocalDateTime now,
            @Param("pending") OutboxStatus pending,
            @Param("sending") OutboxStatus sending
    );
}

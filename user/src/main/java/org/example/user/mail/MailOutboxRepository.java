package org.example.user.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MailOutboxRepository extends JpaRepository<MailOutboxEntity, Long> {

    @Modifying
    @Query("""
            update MailOutboxEntity e
            set e.status = :pending,
                e.lockedAt = null,
                e.nextAttemptAt = :now
            where e.status = :sending
              and e.lockedAt < :staleBefore
            """)
    int recoverStuckSending(
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("now") LocalDateTime now,
            @Param("pending") MailOutboxStatus pending,
            @Param("sending") MailOutboxStatus sending
    );
}

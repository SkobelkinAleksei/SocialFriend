package org.example.scheduler.repository;

import org.example.scheduler.entity.JobStatus;
import org.example.scheduler.entity.ScheduledJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {

    Optional<ScheduledJob> findByUniqueKey(String uniqueKey);

    List<ScheduledJob> findAllByUniqueKeyStartingWith(String prefix);

    @Modifying
    @Query("UPDATE ScheduledJob j SET j.status = org.example.scheduler.entity.JobStatus.PENDING, "
            + "j.lockedAt = null WHERE j.status = org.example.scheduler.entity.JobStatus.RUNNING "
            + "AND j.lockedAt < :staleBefore")
    int recoverStuckRunning(@Param("staleBefore") LocalDateTime staleBefore);

    @Modifying
    @Query("UPDATE ScheduledJob j SET j.status = :status WHERE j.uniqueKey LIKE :prefix "
            + "AND j.status IN :fromStatuses")
    int updateStatusByPrefix(
            @Param("prefix") String prefix,
            @Param("status") JobStatus status,
            @Param("fromStatuses") List<JobStatus> fromStatuses
    );
}

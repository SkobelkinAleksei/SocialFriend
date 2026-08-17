package org.example.user.repository.admin;

import org.example.user.entity.admin.AdminReportCategory;
import org.example.user.entity.admin.AdminReportEntity;
import org.example.user.entity.admin.AdminReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AdminReportRepository extends JpaRepository<AdminReportEntity, Long> {

    Page<AdminReportEntity> findAllByStatusOrderByCreatedAtDesc(AdminReportStatus status, Pageable pageable);

    Page<AdminReportEntity> findAllByStatusAndCategoryOrderByCreatedAtDesc(
            AdminReportStatus status,
            AdminReportCategory category,
            Pageable pageable
    );

    long countByStatus(AdminReportStatus status);

    long countByAccusedId(Long accusedId);

    long countByAccusedIdAndStatusIn(Long accusedId, Collection<AdminReportStatus> statuses);

    boolean existsByReporterIdAndCategoryAndTargetIdAndStatus(
            Long reporterId,
            AdminReportCategory category,
            Long targetId,
            AdminReportStatus status
    );

    List<AdminReportEntity> findAllByAccusedIdOrderByCreatedAtDesc(Long accusedId);

    @Query("""
            select r.accusedId, count(r)
            from AdminReportEntity r
            where r.accusedId in :ids
            group by r.accusedId
            """)
    List<Object[]> countTotalByAccusedIds(@Param("ids") Collection<Long> ids);

    @Query("""
            select r.accusedId, count(r)
            from AdminReportEntity r
            where r.accusedId in :ids
              and r.status in :statuses
            group by r.accusedId
            """)
    List<Object[]> countByAccusedIdsAndStatusIn(
            @Param("ids") Collection<Long> ids,
            @Param("statuses") Collection<AdminReportStatus> statuses
    );

    @Query("""
            select r.accusedId, count(r)
            from AdminReportEntity r
            where r.status in :statuses
            group by r.accusedId
            """)
    List<Object[]> countGroupedByAccusedAndStatusIn(@Param("statuses") Collection<AdminReportStatus> statuses);
}

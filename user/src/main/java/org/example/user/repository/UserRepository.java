package org.example.user.repository;

import org.example.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends
        JpaRepository<UserEntity, Long>,
        JpaSpecificationExecutor<UserEntity>
{
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Query(
            """
                select (count(ue.id) > 0)
                from UserEntity ue
                where ue.email= :email
                or ue.numberPhone= :numberPhone
            """
    )
    boolean isExistByEmailOrNumberPhone(String email, String numberPhone);

    @Query(
            """
                select (count(ue.id) > 0)
                from UserEntity ue
                where lower(ue.email) = lower(:email)
                and ue.id <> :userId
            """
    )
    boolean existsEmailForOtherUser(@Param("email") String email, @Param("userId") Long userId);

    @Query(
            """
                select (count(ue.id) > 0)
                from UserEntity ue
                where ue.numberPhone = :numberPhone
                and ue.id <> :userId
            """
    )
    boolean existsPhoneForOtherUser(@Param("numberPhone") String numberPhone, @Param("userId") Long userId);

    Optional<UserEntity> findByEmail(String email);

    java.util.List<UserEntity> findAllByPlatformRoleOrderByIdAsc(org.example.user.entity.PlatformRole platformRole);

    long countByPlatformRole(org.example.user.entity.PlatformRole platformRole);

    @Modifying
    @Query("UPDATE UserEntity u SET u.reputation = u.reputation + :change WHERE u.id = :userId")
    int updateReputation(@Param("userId") Long userId, @Param("change") long change);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserEntity u SET u.lastSeenAt = :now WHERE u.id = :userId")
    int touchLastSeen(@Param("userId") Long userId, @Param("now") java.time.LocalDateTime now);

    @Query("""
            select u from UserEntity u
            where u.accountStatus = org.example.user.entity.AccountStatus.DELETED
              and u.anonymizedAt is null
              and u.deletedAt < :before
            """)
    java.util.List<UserEntity> findDeletedReadyToAnonymize(@Param("before") java.time.LocalDateTime before);

    @Query("""
            select u from UserEntity u
            where (u.platformRole is null or u.platformRole <> org.example.user.entity.PlatformRole.ADMIN)
              and (u.accountStatus is null or u.accountStatus <> org.example.user.entity.AccountStatus.DELETED)
              and (
                    :q is null or :q = ''
                    or lower(u.firstName) like lower(concat('%', :q, '%'))
                    or lower(u.lastName) like lower(concat('%', :q, '%'))
                    or lower(u.email) like lower(concat('%', :q, '%'))
                    or cast(u.id as string) = :q
                    or lower(concat(u.firstName, ' ', u.lastName)) like lower(concat('%', :q, '%'))
                  )
            """)
    org.springframework.data.domain.Page<UserEntity> searchForAdmin(
            @Param("q") String q,
            org.springframework.data.domain.Pageable pageable
    );
}
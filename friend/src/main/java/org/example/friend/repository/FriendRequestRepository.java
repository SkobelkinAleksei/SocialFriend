package org.example.friend.repository;

import org.example.friend.entity.FriendRequestEntity;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendRequestRepository extends
        JpaRepository<FriendRequestEntity, Long>,
        JpaSpecificationExecutor<FriendRequestEntity>
{
    @Query("""
       select fr from FriendRequestEntity fr
       where (fr.requesterId = :user1 and fr.addresseeId = :user2)
          or (fr.requesterId = :user2 and fr.addresseeId = :user1)
    """)
    Optional<FriendRequestEntity> findRequestBetweenUsers(
            @Param("user1") Long user1,
            @Param("user2") Long user2
    );

    @Query("""
        select fr from FriendRequestEntity fr
        where fr.requesterId = :requesterId and fr.addresseeId = :addresseeId
    """)
    Optional<FriendRequestEntity> findByRequesterIdAndAddresseeId(
            @Param("requesterId") Long requesterId,
            @Param("addresseeId") Long addresseeId
    );

    @Query("""
    SELECT fr.id
    FROM FriendRequestEntity fr
    WHERE (fr.addresseeId = :addresseeId AND fr.requesterId = :requesterId)
       OR (fr.addresseeId = :requesterId AND fr.requesterId = :addresseeId)
""")
    Optional<Long> findRequestIdByAddresseeIdAndRequesterId(
            @Param("addresseeId") Long addresseeId,
            @Param("requesterId") Long requesterId
    );

    long countByAddresseeIdAndStatus(Long addresseeId, FriendRequestStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from FriendRequestEntity fr
            where (fr.requesterId = :userId or fr.addresseeId = :userId)
              and fr.status = :status
            """)
    int deletePendingInvolving(@Param("userId") Long userId, @Param("status") FriendRequestStatus status);
}
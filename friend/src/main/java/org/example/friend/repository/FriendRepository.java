package org.example.friend.repository;

import org.example.friend.entity.FriendEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<FriendEntity, Long> {

    @Query("""
    SELECT fe
    FROM FriendEntity fe
    WHERE fe.userId1 = :userId1 AND fe.userId2 = :userId2
""")
    Optional<FriendEntity> findEntityByUserId1AndUserId2(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

    @Query("""
        SELECT fe
        FROM FriendEntity fe
        WHERE fe.userId1 = :userId OR fe.userId2 = :userId
     """)
    org.springframework.data.domain.Slice<FriendEntity> findAllByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(fe)
        FROM FriendEntity fe
        WHERE fe.userId1 = :userId OR fe.userId2 = :userId
     """)
    long countByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT CASE WHEN fe.userId1 = :userId THEN fe.userId2 ELSE fe.userId1 END
        FROM FriendEntity fe
        WHERE fe.userId1 = :userId OR fe.userId2 = :userId
     """)
    java.util.List<Long> findFriendIdsByUserId(@Param("userId") Long userId);
}
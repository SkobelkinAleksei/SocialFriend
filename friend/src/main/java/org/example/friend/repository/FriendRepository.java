package org.example.friend.repository;

import org.example.friend.entity.FriendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<FriendEntity, Long> {

    @Query("""
    SELECT fe
    FROM FriendEntity fe
    WHERE fe.userId1 = :userId1 AND fe.userId2 = :userId2
""")
    Optional<FriendEntity> findEntityByUserId1AndUserId2(Long userId1, Long userId2);

    @Query("""
        SELECT fe
        FROM FriendEntity fe
        WHERE fe.userId1= :userId OR fe.userId2= :userId
     """)
    List<FriendEntity> findAllByUserId(Long userId);
}
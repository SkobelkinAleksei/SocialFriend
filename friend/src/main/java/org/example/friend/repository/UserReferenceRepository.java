package org.example.friend.repository;

import org.example.friend.entity.UserReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReferenceRepository extends JpaRepository<UserReferenceEntity, Long> {
}
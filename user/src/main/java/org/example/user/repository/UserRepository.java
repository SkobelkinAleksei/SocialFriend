package org.example.user.repository;

import org.example.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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

    Optional<UserEntity> findByEmail(String email);
}
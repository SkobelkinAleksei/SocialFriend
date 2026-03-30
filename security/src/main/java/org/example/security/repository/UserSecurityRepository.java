package org.example.security.repository;

import org.example.security.entity.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, Long> {
    Optional<UserSecurity> findByUsername(String username);
}

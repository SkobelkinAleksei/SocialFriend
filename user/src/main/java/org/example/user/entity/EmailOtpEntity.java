package org.example.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "email_otps")
public class EmailOtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EmailOtpPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @Column(name = "used_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime usedAt;

    @Column(nullable = false)
    private int attempts;
}

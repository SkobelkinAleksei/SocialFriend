package org.example.user.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "number_phone", unique = true, nullable = false)
    private String numberPhone;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(columnDefinition = "DATE", name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "city", nullable = false, length = 50)
    private String city;

    @Column(name = "street_address", nullable = false, length = 150)
    private String streetAddress;

    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;

    @Column(name = "home_latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal homeLatitude;

    @Column(name = "home_longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal homeLongitude;

    @Column(name = "reputation", nullable = false)
    private long reputation = 0L;

    @CreationTimestamp
    @Column(columnDefinition = "TIMESTAMP(0)", name = "time_stamp")
    private LocalDateTime timeStamp;

    @Column(name = "bio", length = 250)
    private String bio;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_mode", length = 20)
    private CoverMode coverMode;

    @Column(name = "cover_color", length = 16)
    private String coverColor;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "last_seen_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 20)
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", length = 20)
    private PlatformRole platformRole;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime deletedAt;

    @Column(name = "banned_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime bannedAt;

    @Column(name = "anonymized_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime anonymizedAt;

    @Column(name = "terms_accepted_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime termsAcceptedAt;

    @Column(name = "terms_version", length = 32)
    private String termsVersion;

    /** Старые аккаунты без колонки считаем подтверждёнными. Новая регистрация ставит false. */
    @Builder.Default
    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean not null default true")
    private Boolean emailVerified = Boolean.TRUE;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private UserSettingsEntity settings;

    public AccountStatus effectiveAccountStatus() {
        return accountStatus == null ? AccountStatus.ACTIVE : accountStatus;
    }

    public boolean isActiveAccount() {
        return effectiveAccountStatus() == AccountStatus.ACTIVE;
    }

    public PlatformRole effectivePlatformRole() {
        return platformRole == null ? PlatformRole.USER : platformRole;
    }

    public boolean isPlatformAdmin() {
        return effectivePlatformRole() == PlatformRole.ADMIN;
    }
}
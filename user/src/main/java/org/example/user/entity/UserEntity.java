package org.example.user.entity;


import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "first_name", nullable = false, length = 20)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 20)
    private String lastName;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "number_phone", unique = true, nullable = false)
    private String numberPhone;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(columnDefinition = "TIMESTAMP", name = "birthday", nullable = false)
    private LocalDate birthday;

    @Column(columnDefinition = "TIMESTAMP", name = "time_stamp")
    private LocalDateTime timeStamp;

    @Column(name = "is_enabled")
    private boolean isEnabled;
}
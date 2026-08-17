package org.example.user;

import com.example.common.dto.event.UserDto;
import org.example.user.dto.RegistrationUserDto;
import org.example.user.dto.UserFullDto;
import org.example.user.entity.PhotoVisibility;
import org.example.user.entity.UserEntity;
import org.example.user.entity.UserSettingsEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Общие заготовки для тестов user-сервиса.
 */
public final class UserFixtures {

    private UserFixtures() {
    }

    public static UserEntity user(long id) {
        return UserEntity.builder()
                .id(id)
                .firstName("Анна")
                .lastName("Иванова")
                .email("anna@example.com")
                .numberPhone("+79001112233")
                .password("$2a$hashed")
                .birthday(LocalDate.of(1995, 5, 10))
                .city("Москва")
                .streetAddress("Тверская 1")
                .districtName("Тверской")
                .homeLatitude(new BigDecimal("55.755800"))
                .homeLongitude(new BigDecimal("37.617300"))
                .reputation(10L)
                .bio("Привет")
                .lastSeenAt(LocalDateTime.now().minusSeconds(10))
                .build();
    }

    public static UserSettingsEntity settings(UserEntity user, PhotoVisibility visibility) {
        UserSettingsEntity settings = UserSettingsEntity.builder()
                .userId(user.getId())
                .user(user)
                .searchRadius(1.0)
                .allowDmFromAll(true)
                .allowCommentsFromAll(true)
                .photoVisibility(visibility)
                .notifyComments(true)
                .notifyMessages(true)
                .notifyEventRequests(true)
                .notifyReputation(true)
                .showLastSeen(true)
                .build();
        user.setSettings(settings);
        return settings;
    }

    public static RegistrationUserDto registration() {
        RegistrationUserDto dto = new RegistrationUserDto();
        dto.setFirstName("Анна");
        dto.setLastName("Иванова");
        dto.setEmail("anna@example.com");
        dto.setNumberPhone("+79001112233");
        dto.setPassword("Secret123");
        dto.setBirthday(LocalDate.of(1995, 5, 10));
        dto.setCity("Москва");
        dto.setStreetAddress("Тверская 1");
        dto.setDistrictName("Тверской");
        dto.setHomeLatitude(new BigDecimal("55.755800"));
        dto.setHomeLongitude(new BigDecimal("37.617300"));
        dto.setAcceptedTerms(true);
        return dto;
    }

    public static UserDto publicDto(long id) {
        UserDto dto = new UserDto();
        dto.setUserId(id);
        dto.setFirstName("Анна");
        dto.setLastName("Иванова");
        return dto;
    }

    public static UserFullDto fullDto(long id) {
        UserFullDto dto = new UserFullDto();
        dto.setId(id);
        dto.setFirstName("Анна");
        dto.setLastName("Иванова");
        return dto;
    }
}

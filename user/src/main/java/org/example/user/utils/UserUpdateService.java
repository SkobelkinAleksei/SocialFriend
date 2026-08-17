package org.example.user.utils;

import com.example.common.dto.event.UserDto;
import lombok.RequiredArgsConstructor;
import org.example.user.dto.UpdatePasswordUserDto;
import org.example.user.dto.UpdateUserDto;
import org.example.user.entity.UserEntity;
import org.example.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static io.micrometer.common.util.StringUtils.isNotBlank;
import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class UserUpdateService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDto updateAccount(UserEntity userEntity, UpdateUserDto updateAccountUser) {

        if (isNull(updateAccountUser)) {
            throw new NullPointerException("UpdateAccountUserDto: " + updateAccountUser + "невозможно обновить!");
        }

        if (isNotBlank(updateAccountUser.getFirstName())) {
            userEntity.setFirstName(updateAccountUser.getFirstName());
        }

        if (isNotBlank(updateAccountUser.getLastName())) {
            userEntity.setLastName(updateAccountUser.getLastName());
        }

        if (isNotBlank(updateAccountUser.getEmail())) {
            userEntity.setEmail(updateAccountUser.getEmail());
        }

        if (isNotBlank(updateAccountUser.getNumberPhone())) {
            userEntity.setNumberPhone(updateAccountUser.getNumberPhone());
        }

        if (updateAccountUser.getBirthday() != null) {
            userEntity.setBirthday(updateAccountUser.getBirthday());
        }

        if (isNotBlank(updateAccountUser.getCity())) {
            userEntity.setCity(updateAccountUser.getCity());
        }
        if (isNotBlank(updateAccountUser.getStreetAddress())) {
            userEntity.setStreetAddress(updateAccountUser.getStreetAddress());
        }
        if (isNotBlank(updateAccountUser.getDistrictName())) {
            userEntity.setDistrictName(updateAccountUser.getDistrictName());
        }
        if (updateAccountUser.getHomeLatitude() != null) {
            userEntity.setHomeLatitude(updateAccountUser.getHomeLatitude());
        }
        if (updateAccountUser.getHomeLongitude() != null) {
            userEntity.setHomeLongitude(updateAccountUser.getHomeLongitude());
        }

        return userMapper.toDto(userEntity);
    }

    public void updatePassword(
            UserEntity userEntity,
            UpdatePasswordUserDto updatePasswordUserDto
    ) {
        if (!passwordEncoder.matches(
                updatePasswordUserDto.getOldPassword(),
                userEntity.getPassword()
        )) {
            throw new IllegalArgumentException("Неверный старый пароль!");
        }
        String newPassword = updatePasswordUserDto.getNewPassword();
        userEntity.setPassword(
                passwordEncoder.encode(newPassword)
        );
    }
}

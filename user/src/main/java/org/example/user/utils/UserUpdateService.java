package org.example.user.utils;

import lombok.RequiredArgsConstructor;
import org.example.user.dto.UpdatePasswordUserDto;
import org.example.user.dto.UpdateUserDto;
import org.example.user.dto.UserDto;
import org.example.user.entity.UserEntity;
import org.example.user.mapper.UserMapper;
import org.example.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static io.micrometer.common.util.StringUtils.isNotBlank;
import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class UserUpdateService {
    private final UserRepository userRepository;
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

        if (isNotBlank(String.valueOf(updateAccountUser.getBirthday()))) {
            userEntity.setBirthday(updateAccountUser.getBirthday());
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

package org.example.user.service;

import org.example.user.dto.*;

import java.util.List;

public interface UserService {
    UserDto signUp(RegistrationUserDto registrationUserDto);
//    UserDto getUserById(Long userId);
    UserDto getUserByEmail(String email);
    UserFullDto getMyProfile(Long userId);
    UserDto updateUserAccount(Long userId, UpdateUserDto updateAccountUser);
    void updatePassword(Long userId, UpdatePasswordUserDto updatePasswordUserDto);
    List<UserDto> searchUsers(UserFilterDto filter, int page, int size);
}

package org.example.user.service;

import com.example.common.dto.event.UserDto;
import org.example.user.dto.*;

import java.util.List;

public interface UserService {
    UserDto signUp(RegistrationUserDto registrationUserDto);
    UserDto getUserById(Long userId);
    UserDto searchUserByEmail(String email);
    UserFullDto getMyProfile(Long userId);
    UserDto updateUserAccount(Long userId, UpdateUserDto updateAccountUser);
    void updatePassword(Long userId, UpdatePasswordUserDto updatePasswordUserDto);
    List<UserDto> searchUsers(Long currentUserId, UserFilterDto filter, int page, int size);
    List<UserDto> getUsersByIds(List<Long> ids);
    void updateReputation(Long userId, long change);
    void updateBio(Long userId, UpdateBioDto newBio);
    UserDto getUserProfileForViewer(Long userId, Long viewerId);
    void heartbeat(Long userId);
    java.util.Map<Long, UserPresenceDto> getPresence(List<Long> ids);
    void deleteMyAccount(Long userId);
    void banUser(Long userId);
    void unbanUser(Long userId);
    String accountStatus(Long userId);
}

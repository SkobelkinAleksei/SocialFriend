package org.example.chat.service;

import org.example.chat.entity.AddGroupMembersRequest;
import org.example.chat.entity.ChatMemberDto;
import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatMessageDto;
import org.example.chat.entity.ChatPhotoPageDto;
import org.example.chat.entity.ChatRoomDto;
import org.example.chat.entity.CreatePersonalGroupRequest;
import org.example.chat.entity.UpdatePersonalGroupRequest;

import java.util.List;

public interface GroupChatService {
    void joinToGroupRoom(Long eventId, Long userId);
    List<ChatRoomDto> getMyGroupRooms(Long userId);
    List<ChatMessageDto> getGroupChatHistory(Long chatId, Long userId);
    List<ChatMessageDto> getGroupChatHistoryPage(Long chatId, Long userId, Long beforeId, int size);
    ChatPhotoPageDto getGroupChatPhotos(Long chatId, Long userId, int page, int size);
    ChatPhotoPageDto getGroupChatFiles(Long chatId, Long userId, int page, int size);
    ChatPhotoPageDto getGroupChatVoices(Long chatId, Long userId, int page, int size);
    void joinToGroupRoomAndNotify(Long eventId, Long userId, String firstName, String lastName);
    Long createGroupRoom(Long eventId, String title, Long ownerId);
    void resetUnreadCount(Long chatId, Long userId);
    void clearGroupChatForUser(Long eventId, Long userId);
    void leaveGroupRoom(Long eventId, Long userId, String firstName, String lastName);
    void deleteGroupMessagesBatch(List<Long> messageIds, Long senderId);
    void cancelAndNotifyGroupRoom(Long eventId, Long ownerId);
    void kickParticipantFromChatRoom(Long eventId, Long userId, String firstName, String lastName);
    void postSystemMessageByEventId(Long eventId, String content);
    void createKeepChatPollByEventId(Long eventId, com.example.common.dto.scheduler.KeepChatPollRequest request);
    boolean resolveKeepChatPollByEventId(Long eventId);
    void closeRoomByEventId(Long eventId);
    void deleteRoomByEventId(Long eventId);

    ChatRoomDto createPersonalGroup(Long ownerId, String firstName, String lastName, CreatePersonalGroupRequest request);
    ChatRoomDto updatePersonalGroup(Long chatId, Long userId, String firstName, String lastName, UpdatePersonalGroupRequest request);
    List<ChatMemberDto> listMembers(Long chatId, Long userId);
    void addMembers(Long chatId, Long actorId, String firstName, String lastName, AddGroupMembersRequest request);
    void leavePersonalGroup(Long chatId, Long userId, String firstName, String lastName);
    void kickPersonalGroupMember(Long chatId, Long ownerId, Long targetUserId, String firstName, String lastName);
    void setMemberAdmin(Long chatId, Long actorId, Long targetUserId, boolean admin, String firstName, String lastName);
    ChatRoomDto transferPersonalGroup(Long chatId, Long ownerId, Long newOwnerId, String firstName, String lastName);
    void deletePersonalGroup(Long chatId, Long ownerId);
    void kickGroupMember(Long chatId, Long actorId, Long targetUserId, String firstName, String lastName);
}

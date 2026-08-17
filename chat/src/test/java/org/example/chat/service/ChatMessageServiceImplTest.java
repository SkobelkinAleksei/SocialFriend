package org.example.chat.service;

import org.example.chat.entity.ChatMessage;
import org.example.chat.entity.ChatPhotoItemDto;
import org.example.chat.entity.ChatPhotoPageDto;
import org.example.chat.repository.ChatMessageRepository;
import org.example.chat.repository.ChatParticipantRepository;
import org.example.chat.repository.ChatRoomRepository;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.metrics.AppMetrics;
import org.example.restclient.config.IHttpCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageServiceImpl — свои сообщения и пагинация медиа")
public class ChatMessageServiceImplTest {

    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private IHttpCore httpCore;
    @Mock
    private NotificationKafkaProducer notificationProducer;
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatPreferenceService preferenceService;
    @Mock
    private ChatPinService pinService;
    @Mock
    private AppMetrics appMetrics;

    @InjectMocks
    private ChatMessageServiceImpl service;

    @Test
    @DisplayName("Чужое сообщение нельзя редактировать")
    void editForeign() {
        ChatMessage message = ChatMessage.builder().id(1L).senderId(2L).content("hi").build();
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThrows(SecurityException.class, () -> service.editMessage(1L, "новое", 9L));
    }

    @Test
    @DisplayName("Автор редактирует: edited=true, текст меняется")
    void editOwn() {
        ChatMessage message = ChatMessage.builder().id(1L).senderId(2L).content("hi").build();
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);

        ChatMessage saved = service.editMessage(1L, "привет сосед", 2L);

        assertEquals("привет сосед", saved.getContent());
        assertTrue(saved.isEdited());
    }

    @Test
    @DisplayName("Чужое сообщение нельзя удалить")
    void deleteForeign() {
        ChatMessage message = ChatMessage.builder().id(1L).senderId(2L).build();
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        assertThrows(SecurityException.class, () -> service.deleteMessage(1L, 9L));
    }

    @Test
    @DisplayName("Своё удаление — soft delete + снятие пина")
    void deleteOwn() {
        ChatMessage message = ChatMessage.builder().id(1L).senderId(2L).deleted(false).build();
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        service.deleteMessage(1L, 2L);

        assertTrue(message.isDeleted());
        verify(pinService).removePinForDeletedMessage(1L);
    }

    @Test
    @DisplayName("paginatePhotoItems: пустая страница за концом списка, hasMore=false")
    void paginatePastEnd() {
        List<ChatPhotoItemDto> items = List.of(new ChatPhotoItemDto(), new ChatPhotoItemDto());
        ChatPhotoPageDto page = ChatMessageServiceImpl.paginatePhotoItems(items, 5, 10);
        assertTrue(page.getItems().isEmpty());
        assertEquals(2, page.getTotal());
        assertEquals(false, page.isHasMore());
    }
}

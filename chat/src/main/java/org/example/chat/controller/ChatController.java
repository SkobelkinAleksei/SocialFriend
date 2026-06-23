package org.example.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.chat.entity.ChatMessage;
import org.example.chat.service.ChatMessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService messageService;

    // 1. WebSocket обработчик (Фронтенд будет слать фреймы на /app/chat)
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        chatMessage.setTimestamp(LocalDateTime.now());
        ChatMessage savedMsg = messageService.saveMessage(chatMessage);

        // Отправляем сообщение получателю в его персональную WebSocket-очередь
        // На фронте получатель подпишется на /user/{myId}/queue/messages
        messagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getRecipientId()),
                "/queue/messages",
                savedMsg
        );
    }

    // 2. REST эндпоинт для первой загрузки истории при открытии диалога
    @GetMapping("/api/v1/social/chats/history/{recipientId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(
            @RequestHeader("X-User-Id") Long senderId,
            @PathVariable Long recipientId) {

        return messageService.getChatHistory(senderId, recipientId);
    }

    @GetMapping("/api/v1/social/chats/unread/count")
    @ResponseBody
    public long getUnreadMessagesCount(@RequestHeader("X-User-Id") Long userId) {
        return messageService.getUnreadCount(userId);
    }

    @PutMapping("/api/v1/social/chats/read/{senderId}")
    @ResponseBody
    public void readMessages(
            @PathVariable Long senderId,
            @RequestHeader("X-User-Id") Long recipientId) {
        messageService.readAllMessagesFromUser(senderId, recipientId);
    }

    // Добавьте новые методы в ChatController.java

    // 1. РЕДАКТИРОВАНИЕ СООБЩЕНИЯ
    @PutMapping("/api/v1/social/chats/message/{messageId}")
    @ResponseBody
    public ChatMessage editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent, // фронтенд пришлет просто строку в body
            @RequestHeader("X-User-Id") Long senderId) {

        ChatMessage updatedMsg = messageService.editMessage(messageId, newContent, senderId);

        // Оповещаем получателя через вебсокет, что сообщение было изменено.
        // Отправляем специальный системный заголовок или просто обновленный объект
        messagingTemplate.convertAndSendToUser(
                String.valueOf(updatedMsg.getRecipientId()),
                "/queue/messages",
                updatedMsg // фронтенд найдет id сообщения и заменит текст
        );

        return updatedMsg;
    }

    // 2. УДАЛЕНИЕ ОДНОГО СООБЩЕНИЯ
    @DeleteMapping("/api/v1/social/chats/message/{messageId}")
    @ResponseBody
    public void deleteMessage(
            @PathVariable Long messageId,
            @RequestHeader("X-User-Id") Long senderId) {

        // Сначала ищем сообщение, чтобы узнать получателя перед удалением
        ChatMessage msg = messageService.getChatHistory(senderId, senderId).stream() // временный поиск или добавьте findById в сервис
                .filter(m -> m.getId().equals(messageId)).findFirst().orElse(null);

        messageService.deleteMessage(messageId, senderId);

        if (msg != null) {
            // Отправляем системное уведомление получателю, что сообщение с таким ID нужно стереть с экрана
            // Мы можем послать объект, у которого контент равен, например, "[DELETED]"
            msg.setContent("[DELETED]");
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(msg.getRecipientId()),
                    "/queue/messages",
                    msg
            );
        }
    }

    // 3. ПОЛНАЯ ОЧИСТКА ДИАЛОГА
    @DeleteMapping("/api/v1/social/chats/clear/{recipientId}")
    @ResponseBody
    public void clearChat(
            @PathVariable Long recipientId,
            @RequestHeader("X-User-Id") Long senderId) {

        messageService.clearChat(senderId, recipientId);

        // Посылаем сигнал собеседнику, что чат был полностью очищен
        ChatMessage clearSignal = ChatMessage.builder()
                .senderId(senderId)
                .recipientId(recipientId)
                .content("[CLEAR_CHAT]")
                .build();

        messagingTemplate.convertAndSendToUser(
                String.valueOf(recipientId),
                "/queue/messages",
                clearSignal
        );
    }

    @GetMapping("/api/v1/social/chats/users")
    @ResponseBody
    public List<Long> getChatUsers(@RequestHeader("X-User-Id") Long userId) {
        return messageService.getDistinctChatPartnerIds(userId);
    }
}

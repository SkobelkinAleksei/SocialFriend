package org.example.notification.util;

import com.example.common.kafka.NotificationGroupDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.entity.NotificationEntity;
import org.example.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAggregator {
    private final NotificationMapper notificationMapper;

    /** Агрегирует и группирует список уведомлений в хронологическом порядке */
    public List<NotificationGroupDto> aggregateNotifications(List<NotificationEntity> entities) {
        Map<String, List<NotificationEntity>> groupedMap = entities.stream()
                .collect(Collectors.groupingBy(
                        entity -> {
                            // Если уведомление УЖЕ прочитано, оно НИКОГДА не группируется
                            if (entity.isRead()) {
                                return "READ_" + entity.getId();
                            }
                            // Логика группировки для НЕПРОЧИТАННЫХ уведомлений по типам
                            return switch (entity.getType()) {
                                case FRIEND_REQUEST_SENT -> "UNREAD_FRIEND_REQUEST_SENT";
                                case FRIEND_REQUEST_ACCEPTED -> "UNREAD_FRIEND_REQUEST_ACCEPTED";
                                // Лайки и комментарии — отдельные группы, каждая в рамках своего поста
                                case POST_LIKE -> "UNREAD_POST_LIKE_" + entity.getTargetId();
                                case PHOTO_LIKE -> "UNREAD_PHOTO_LIKE_" + entity.getContextLabel();
                                case NEW_COMMENT -> "UNREAD_POST_COMMENT_" + entity.getTargetId();
                                case COMMENT_REPLY -> "UNREAD_COMMENT_REPLY_" + entity.getTargetId();
                                case NEW_CHAT_MESSAGE -> "UNREAD_CHAT_" + entity.getTargetId();
                                case EVENT_JOIN_REQUEST ->
                                        "UNREAD_EVENT_" + entity.getTargetId() + "_FROM_" + entity.getSenderId();
                                case EVENT_JOIN_SUCCESS, EVENT_JOIN_REJECTED, EVENT_JOIN_BANNED, EVENT_KICK ->
                                        "UNREAD_" + entity.getType() + "_" + entity.getTargetId() + "_FROM_" + entity.getSenderId();
                                default -> "UNREAD_UNIQUE_" + entity.getId();
                            };
                        },

                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<NotificationGroupDto> result = new ArrayList<>();
        for (Map.Entry<String, List<NotificationEntity>> entry : groupedMap.entrySet()) {
            List<NotificationEntity> groupList = entry.getValue();
            // Так как список отсортирован по убыванию, первый элемент в группе — самый СВЕЖИЙ
            NotificationEntity newest = groupList.get(0);
            int count = groupList.size();

            List<NotificationGroupDto.NotificationItemDetails> itemDetails = groupList.stream()
                    .map(notificationMapper::toItemDetails)
                    .toList();

            // Собираем ID всех записей, вошедших в эту группу
            List<Long> mergedIds = groupList.stream()
                    .map(NotificationEntity::getId)
                    .toList();

            // Формируем красивый динамический текст в зависимости от размера группы
            String finalMessage = newest.getMessage();
            if (count > 1 && !newest.isRead()) {
                finalMessage = buildGroupMessage(newest, count);
            }

            NotificationGroupDto dto = notificationMapper.toGroupDto(newest);
            dto.setMessage(finalMessage);
            dto.setCount(count);
            dto.setMergedIds(mergedIds);
            dto.setItems(itemDetails);

            result.add(dto);
        }

        log.info("[DEBUG ГРУППИРОВКА] Сформировано групп: {}. Исходных записей было: {}", result.size(), entities.size());
        result.forEach(g -> log.info("Группа ID: {}, Тип: {}, Count: {}, Текст: {}", g.getId(), g.getType(), g.getCount(), g.getMessage()));
        return result;
    }

    /** Создает текст для сгруппированного уведомления */
    private String buildGroupMessage(NotificationEntity newest, int count) {
        String lastName = newest.getSenderLastName() != null ? newest.getSenderLastName() : "";

        return switch (newest.getType()) {
            case FRIEND_REQUEST_SENT ->
                    "%s %s и еще %d чел. хотят добавиться в друзья".formatted(newest.getSenderFirstName(), lastName, count - 1);
            case FRIEND_REQUEST_ACCEPTED ->
                    "%s %s и еще %d чел. приняли ваши запросы в друзья".formatted(newest.getSenderFirstName(), lastName, count - 1);
            case POST_LIKE ->
                    "%s %s и еще %d чел. оценили вашу публикацию".formatted(newest.getSenderFirstName(), lastName, count - 1);
            case PHOTO_LIKE ->
                    "%s %s и еще %d чел. лайкнули ваше фото".formatted(newest.getSenderFirstName(), lastName, count - 1);
            case NEW_COMMENT ->
                    "%s %s и еще %d чел. прокомментировали ваш пост".formatted(newest.getSenderFirstName(), lastName, count - 1);
            case COMMENT_REPLY ->
                    "%s %s и еще %d чел. ответили вам в комментариях".formatted(newest.getSenderFirstName(), lastName, count - 1);
            case NEW_CHAT_MESSAGE ->
                    "%s %s и еще %d сообщ. в этом чате".formatted(newest.getSenderFirstName(), lastName, count - 1);
            case EVENT_JOIN_REQUEST ->
                    "%s %s и еще %d чел. хотят присоединиться к событию".formatted(newest.getSenderFirstName(), lastName, count - 1);
            default -> newest.getMessage();
        };
    }
}
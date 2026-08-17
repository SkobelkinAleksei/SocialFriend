package com.example.common.lifecycle;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDateTime;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.lifecycle")
public class EventLifecycleSettings {

    /** За сколько минут до начала напомнить в чат. Боевое значение: 60. */
    private int remindMinutesBefore = 60;
    /** Через сколько минут после начала открыть оценки. Боевое: 180. */
    private int reputationOpenMinutes = 180;
    /** Через сколько минут после начала закрыть оценки. Боевое: 10080 (7 суток). */
    private int reputationCloseMinutes = 7 * 24 * 60;
    /** Через сколько минут после начала запустить опрос про чат. Боевое: 1440. */
    private int keepPollMinutesAfterStart = 24 * 60;
    /** Сколько минут длится опрос про чат. Боевое: 1440. */
    private int keepResolveMinutesAfterPoll = 24 * 60;
    /** Через сколько минут после отмены удалить чат. Боевое: 720. */
    private int cancelDeleteMinutes = 12 * 60;
    /** Через сколько минут после провала опроса удалить чат. Боевое: 5. */
    private int keepFailDeleteMinutes = 5;
    private int keepYesPercent = 65;
    private int minPeople = 2;

    public LocalDateTime remindAt(LocalDateTime eventDate) {
        return eventDate.minusMinutes(remindMinutesBefore);
    }

    public LocalDateTime reputationOpensAt(LocalDateTime eventDate) {
        return eventDate.plusMinutes(reputationOpenMinutes);
    }

    public LocalDateTime reputationClosesAt(LocalDateTime eventDate) {
        return eventDate.plusMinutes(reputationCloseMinutes);
    }

    public LocalDateTime keepPollAt(LocalDateTime eventDate) {
        return eventDate.plusMinutes(keepPollMinutesAfterStart);
    }

    public LocalDateTime keepResolveAt(LocalDateTime eventDate) {
        return keepPollAt(eventDate).plusMinutes(keepResolveMinutesAfterPoll);
    }

    public boolean isReputationOpen(LocalDateTime eventDate, LocalDateTime now) {
        if (eventDate == null || now == null) {
            return false;
        }
        return !now.isBefore(reputationOpensAt(eventDate)) && now.isBefore(reputationClosesAt(eventDate));
    }

    public boolean keepChat(int yesCount, int eligibleCount) {
        if (eligibleCount < minPeople) {
            return false;
        }
        return yesCount * 100 >= eligibleCount * keepYesPercent;
    }

    public String minutesLabel(int minutes) {
        if (minutes > 0 && minutes % 60 == 0) {
            int hours = minutes / 60;
            if (hours % 24 == 0) {
                int days = hours / 24;
                return days + " " + daysWord(days);
            }
            return hours + " " + hoursWord(hours);
        }
        return minutes + " " + minutesWord(minutes);
    }

    private static String minutesWord(int n) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) return "минут";
        if (mod10 == 1) return "минуту";
        if (mod10 >= 2 && mod10 <= 4) return "минуты";
        return "минут";
    }

    private static String hoursWord(int n) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) return "часов";
        if (mod10 == 1) return "час";
        if (mod10 >= 2 && mod10 <= 4) return "часа";
        return "часов";
    }

    private static String daysWord(int n) {
        int mod100 = n % 100;
        int mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) return "суток";
        if (mod10 == 1) return "сутки";
        return "суток";
    }
}

package org.example.chat.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatMentions {
    private static final Pattern PATTERN = Pattern.compile("\\[MENTION:(\\d+)\\]");

    private ChatMentions() {
    }

    public static List<Long> parse(String content) {
        List<Long> ids = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return ids;
        }
        Matcher matcher = PATTERN.matcher(content);
        while (matcher.find()) {
            try {
                Long id = Long.valueOf(matcher.group(1));
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }
}

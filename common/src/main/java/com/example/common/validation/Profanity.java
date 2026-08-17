package com.example.common.validation;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class Profanity {

    private static final List<String> RU_STEMS = List.of(
            "хуй", "хуя", "хуе", "хуи", "хуё",
            "пизд", "пезд",
            "ебан", "ебат", "еблан", "ебло", "ебуч", "ёбан", "ёбат", "ёбуч",
            "бляд", "блять", "блядь", "блят",
            "мудак", "мудил",
            "залуп",
            "пидор", "пидр", "педик",
            "гандон",
            "нахуй", "похуй", "охуе", "ахуе",
            "спизд", "выеб", "заеб", "уебок", "уёб",
            "сука", "сучар"
    );

    private static final Set<String> EN_WORDS = Set.of(
            "fuck", "fucking", "fucker", "fucked", "motherfucker",
            "shit", "shitty", "asshole", "bitch", "cunt",
            "dick", "cock", "pussy", "bastard", "slut", "whore",
            "nigger", "nigga",
            "blyat", "blyad", "pizda", "pidor", "suka", "huy", "nahuy"
    );

    private Profanity() {
    }

    /** Найденный фрагмент или null, если текст чистый. */
    public static String findMatch(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT).replace('ё', 'е');
        String spaced = normalized.replaceAll("[^a-zа-я0-9]+", " ").trim();
        if (spaced.isEmpty()) {
            return null;
        }
        for (String word : EN_WORDS) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
            if (p.matcher(spaced).find()) {
                return word;
            }
        }
        String compact = spaced.replace(" ", "");
        for (String stem : RU_STEMS) {
            String s = stem.replace('ё', 'е');
            if (compact.contains(s)) {
                return stem;
            }
        }
        return null;
    }

    public static boolean contains(String raw) {
        return findMatch(raw) != null;
    }
}

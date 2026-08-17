package com.example.common.validation;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Объявления купли-продажи во встречах. Не путать с «купим кофе» или «забрать бесплатно».
 */
public final class MarketplaceListing {

    public static final String MESSAGE =
            "Сервис не для объявлений о купле-продаже. Встреча — это совместное дело, а не витрина.";

    private static final List<String> SELL_WORDS = List.of(
            "продам", "продаю", "продаем", "продадим", "продадите", "продашь", "продаете",
            "продажа", "продаже", "продажей", "продажу", "продаж",
            "продается", "продаются",
            "продать", "продавать",
            "перепродам", "перепродаю"
    );

    private static final List<String> BUY_LISTING_WORDS = List.of(
            "куплю", "скуплю", "выкуплю", "прикуплю"
    );

    private static final Pattern PAID_GIVEAWAY = Pattern.compile(
            "(?:^| )(?:отдам|отдаю|отдаем)(?: [а-яa-z0-9]+){0,6} за \\d"
    );

    private MarketplaceListing() {
    }

    public static boolean isListing(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String spaced = raw.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^a-zа-я0-9]+", " ")
                .trim();
        if (spaced.isEmpty()) {
            return false;
        }
        if (hasAnyWord(spaced, SELL_WORDS) || hasAnyWord(spaced, BUY_LISTING_WORDS)) {
            return true;
        }
        if (hasWord(spaced, "торг")) {
            return true;
        }
        return PAID_GIVEAWAY.matcher(" " + spaced + " ").find();
    }

    private static boolean hasAnyWord(String spaced, List<String> words) {
        for (String word : words) {
            if (hasWord(spaced, word)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasWord(String spaced, String word) {
        int from = 0;
        while (from <= spaced.length() - word.length()) {
            int i = spaced.indexOf(word, from);
            if (i < 0) {
                return false;
            }
            boolean leftOk = i == 0 || !isWordChar(spaced.charAt(i - 1));
            int end = i + word.length();
            boolean rightOk = end >= spaced.length() || !isWordChar(spaced.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            from = i + 1;
        }
        return false;
    }

    private static boolean isWordChar(char c) {
        return (c >= 'а' && c <= 'я') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }
}

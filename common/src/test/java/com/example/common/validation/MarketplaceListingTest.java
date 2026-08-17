package com.example.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MarketplaceListing — объявления купли-продажи во встречах")
public class MarketplaceListingTest {

    @Test
    @DisplayName("Пустой текст не объявление")
    void blank() {
        assertFalse(MarketplaceListing.isListing(null));
        assertFalse(MarketplaceListing.isListing("  "));
    }

    @Test
    @DisplayName("Продам / куплю / торг — объявление")
    void classicListing() {
        assertTrue(MarketplaceListing.isListing("Продам диван"));
        assertTrue(MarketplaceListing.isListing("Куплю дешево велик, торг"));
        assertTrue(MarketplaceListing.isListing("Продажа велосипеда, состояние отличное"));
        assertTrue(MarketplaceListing.isListing("Отдам за 3000 рублей"));
    }

    @Test
    @DisplayName("SOS: вынести / забрать бесплатно — не объявление")
    void sosGiveaway() {
        assertFalse(MarketplaceListing.isListing("Нужно помочь вынести диван на мусорку"));
        assertFalse(MarketplaceListing.isListing("Можно забрать диван бесплатно"));
        assertFalse(MarketplaceListing.isListing("Отдам даром, самовывоз"));
    }

    @Test
    @DisplayName("Тусовка: купим кофе, дешевое, всем бесплатно — не объявление")
    void socialHangout() {
        assertFalse(MarketplaceListing.isListing("Потусуемся в парке, купим кофе, там дешевое"));
        assertFalse(MarketplaceListing.isListing("Встреча во дворе, всем бесплатно"));
        assertFalse(MarketplaceListing.isListing("Скинемся, купим чай и печенье"));
    }

    @Test
    @DisplayName("«торговый центр» не ловится из‑за слова торг")
    void shoppingCenterNotFlagged() {
        assertFalse(MarketplaceListing.isListing("Встречаемся у торгового центра"));
    }
}

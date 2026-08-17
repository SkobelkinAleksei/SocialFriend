package com.example.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * Счётчики продукта. Имена метрик — латиница (так требует Prometheus),
 * описания на русском — их видно в Grafana.
 */
public class AppMetrics {

    // Prometheus сам добавит суффикс _total у счётчиков и _seconds у таймера.
    public static final String VHOD_USPEH = "myraion_vhod_uspeh";
    public static final String VHOD_OSHIBKA = "myraion_vhod_oshibka";
    public static final String VHOD_BLOKIROVKA = "myraion_vhod_blokirovka";
    public static final String REGISTRACIYA_USPEH = "myraion_registraciya_uspeh";
    public static final String REGISTRACIYA_OSHIBKA = "myraion_registraciya_oshibka";
    public static final String CHAT_LICHNOE = "myraion_chat_soobshenie_lichnoe";
    public static final String CHAT_GRUPPA = "myraion_chat_soobshenie_gruppa";
    public static final String CHAT_OSHIBKA = "myraion_chat_soobshenie_oshibka";
    public static final String CHAT_WS_ON = "myraion_chat_ws_podklyuchenie";
    public static final String CHAT_WS_OFF = "myraion_chat_ws_otklyuchenie";
    public static final String CHAT_ISTORIYA_USPEH = "myraion_chat_istoriya_uspeh";
    public static final String CHAT_ISTORIYA_OSHIBKA = "myraion_chat_istoriya_oshibka";
    public static final String VSTRECHA_SOZDANA = "myraion_vstrecha_sozdana";
    public static final String VSTRECHA_ZAYAVKA = "myraion_vstrecha_zayavka";
    public static final String VSTRECHA_VSTUPLENIE = "myraion_vstrecha_vstuplenie";
    public static final String VSTRECHA_SPISOK_OSHIBKA = "myraion_vstrecha_spisok_oshibka";
    public static final String UVEDOMLENIE_SOHRANENO = "myraion_uvedomlenie_sohraneno";
    public static final String UVEDOMLENIE_OTPRAVLENO = "myraion_uvedomlenie_otpravleno";
    public static final String UVEDOMLENIE_OSHIBKA = "myraion_uvedomlenie_oshibka";
    public static final String OUTBOX_ZAPISANO = "myraion_outbox_zapisano";
    public static final String OUTBOX_OTPRAVLENO = "myraion_outbox_otpravleno";
    public static final String OUTBOX_OSHIBKA = "myraion_outbox_oshibka";
    public static final String DZHOB_USPEH = "myraion_dzhob_uspeh";
    public static final String DZHOB_OSHIBKA = "myraion_dzhob_oshibka";
    public static final String SHLYUZ_OSHIBKA = "myraion_shlyuz_oshibka";
    public static final String SHLYUZ_MEDLENNO = "myraion_shlyuz_medlennyy_zapros";
    public static final String SHLYUZ_VREMYA = "myraion_shlyuz_zapros";

    private final MeterRegistry registry;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void vhodUspeh() {
        inc(VHOD_USPEH, "Успешный вход в аккаунт");
    }

    public void vhodOshibka() {
        inc(VHOD_OSHIBKA, "Неудачный вход (неверный пароль или логин)");
    }

    public void vhodBlokirovka() {
        inc(VHOD_BLOKIROVKA, "Аккаунт временно заблокирован после неудачных входов");
    }

    public void registraciyaUspeh() {
        inc(REGISTRACIYA_USPEH, "Новая регистрация прошла успешно");
    }

    public void registraciyaOshibka() {
        inc(REGISTRACIYA_OSHIBKA, "Ошибка регистрации");
    }

    public void chatLichnoe() {
        inc(CHAT_LICHNOE, "Отправлено личное сообщение");
    }

    public void chatGruppa() {
        inc(CHAT_GRUPPA, "Отправлено сообщение в групповой чат");
    }

    public void chatOshibka() {
        inc(CHAT_OSHIBKA, "Не удалось отправить сообщение");
    }

    public void chatWsPodklyuchenie() {
        inc(CHAT_WS_ON, "Подключение к чату по WebSocket");
    }

    public void chatWsOtklyuchenie() {
        inc(CHAT_WS_OFF, "Отключение от чата (WebSocket)");
    }

    public void chatIstoriyaUspeh() {
        inc(CHAT_ISTORIYA_USPEH, "История чата загружена");
    }

    public void chatIstoriyaOshibka() {
        inc(CHAT_ISTORIYA_OSHIBKA, "Ошибка загрузки истории чата");
    }

    public void vstrechaSozdana() {
        inc(VSTRECHA_SOZDANA, "Создана встреча");
    }

    public void vstrechaZayavka() {
        inc(VSTRECHA_ZAYAVKA, "Заявка на приватную встречу");
    }

    public void vstrechaVstuplenie() {
        inc(VSTRECHA_VSTUPLENIE, "Человек вступил во встречу");
    }

    public void vstrechaSpisokOshibka() {
        inc(VSTRECHA_SPISOK_OSHIBKA, "Ошибка загрузки списка встреч на карте");
    }

    public void uvedomlenieSohraneno() {
        inc(UVEDOMLENIE_SOHRANENO, "Уведомление записано в базу");
    }

    public void uvedomlenieOtpravleno() {
        inc(UVEDOMLENIE_OTPRAVLENO, "Уведомление ушло в сокет");
    }

    public void uvedomlenieOshibka() {
        inc(UVEDOMLENIE_OSHIBKA, "Не удалось отправить уведомление");
    }

    public void outboxZapisano() {
        inc(OUTBOX_ZAPISANO, "Событие записано в outbox");
    }

    public void outboxOtpravleno() {
        inc(OUTBOX_OTPRAVLENO, "Событие из outbox ушло в Kafka");
    }

    public void outboxOshibka() {
        inc(OUTBOX_OSHIBKA, "Ошибка отправки события из outbox");
    }

    public void dzhobUspeh() {
        inc(DZHOB_USPEH, "Задача планировщика выполнена");
    }

    public void dzhobOshibka() {
        inc(DZHOB_OSHIBKA, "Задача планировщика упала");
    }

    public void shlyuzOshibka(int status) {
        String klass = status >= 500 ? "5xx" : "4xx";
        Counter.builder(SHLYUZ_OSHIBKA)
                .description("Ошибка запроса через шлюз")
                .tag("klass", klass)
                .tag("kod", String.valueOf(status))
                .register(registry)
                .increment();
    }

    public void shlyuzMedlenno() {
        inc(SHLYUZ_MEDLENNO, "Медленный запрос через шлюз (дольше 2 секунд)");
    }

    public void shlyuzVremya(Duration duration) {
        Timer.builder(SHLYUZ_VREMYA)
                .description("Время ответа шлюза")
                .publishPercentileHistogram()
                .register(registry)
                .record(duration);
    }

    private void inc(String name, String descriptionRu) {
        Counter.builder(name)
                .description(descriptionRu)
                .register(registry)
                .increment();
    }
}

package org.example.notification.service;

import com.example.common.kafka.NotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.example.notification.dto.PushSubscribeRequest;
import org.example.notification.entity.PushSubscriptionEntity;
import org.example.notification.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.Security;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class WebPushService {

    private final PushSubscriptionRepository repository;
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final PushService pushService;

    public WebPushService(
            PushSubscriptionRepository repository,
            ObjectMapper objectMapper,
            @Value("${app.vapid.public-key:}") String publicKey,
            @Value("${app.vapid.private-key:}") String privateKey,
            @Value("${app.vapid.subject:mailto:myraion@inbox.ru}") String subject
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.publicKey = publicKey == null ? "" : publicKey.trim();
        String priv = privateKey == null ? "" : privateKey.trim();
        PushService service = null;
        if (StringUtils.hasText(this.publicKey) && StringUtils.hasText(priv)) {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                service = new PushService(this.publicKey, priv, subject);
            } catch (Exception ex) {
                log.warn("[Push] Не подняли VAPID: {}", ex.getMessage());
            }
        } else {
            log.info("[Push] VAPID ключи пустые — баннеры на телефон выключены");
        }
        this.pushService = service;
    }

    public Optional<String> publicKey() {
        if (pushService == null || !StringUtils.hasText(publicKey)) {
            return Optional.empty();
        }
        return Optional.of(publicKey);
    }

    @Transactional
    public void subscribe(Long userId, PushSubscribeRequest request) {
        if (pushService == null) {
            return;
        }
        PushSubscriptionEntity row = repository.findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscriptionEntity::new);
        row.setUserId(userId);
        row.setEndpoint(request.getEndpoint());
        row.setP256dh(request.getP256dh());
        row.setAuth(request.getAuth());
        repository.save(row);
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return;
        }
        repository.deleteByUserIdAndEndpoint(userId, endpoint.trim());
    }

    @Async
    @Transactional
    public void sendToUser(Long userId, NotificationDto dto) {
        if (pushService == null || dto == null) {
            return;
        }
        List<PushSubscriptionEntity> subs = repository.findAllByUserId(userId);
        if (subs.isEmpty()) {
            return;
        }
        byte[] payload;
        try {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("title", "На районе");
            String text = StringUtils.hasText(dto.getMessage()) ? dto.getMessage() : "На районе";
            body.put("body", text);
            body.put("url", "/");
            payload = objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            log.warn("[Push] Не собрали payload: {}", ex.getMessage());
            return;
        }
        for (PushSubscriptionEntity sub : subs) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload
                );
                HttpResponse response = pushService.send(notification);
                int status = response.getStatusLine().getStatusCode();
                if (status == HttpStatus.GONE.value() || status == HttpStatus.NOT_FOUND.value()) {
                    repository.deleteByEndpoint(sub.getEndpoint());
                } else if (status >= 400) {
                    log.warn("[Push] HTTP {} для user={}", status, userId);
                }
            } catch (Exception ex) {
                log.warn("[Push] Не отправили user={}: {}", userId, ex.getMessage());
            }
        }
    }
}

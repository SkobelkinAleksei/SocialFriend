package org.example.user.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class NeighborhoodMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost = "";

    @Value("${app.mail.from:}")
    private String from = "";

    @Value("${spring.mail.username:}")
    private String username = "";

    @Value("${app.mail.log-codes:false}")
    private boolean logCodes;

    public NeighborhoodMailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCode(String to, String subject, String body, String plainCode) {
        if (logCodes) {
            log.info("[Почта] Код для {} (log-codes): {}", to, plainCode);
        }
        if (!StringUtils.hasText(mailHost) || mailSender == null) {
            log.warn("[Почта] SMTP не настроен (MAIL_HOST пустой) — письмо на {} не ушло", to);
            throw new IllegalStateException("Сервер ещё не умеет слать письма. Напишите в поддержку района.");
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "UTF-8");
            String sender = StringUtils.hasText(from) ? from : username;
            if (StringUtils.hasText(sender)) {
                helper.setFrom(sender);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(mime);
            log.info("[Почта] Письмо отправлено на {}", to);
        } catch (Exception ex) {
            log.warn("[Почта] Не отправили на {}: {}", to, ex.getMessage());
            throw new IllegalStateException("Не получилось отправить письмо. Попробуйте через минуту.");
        }
    }
}

package org.example.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
        if (logCodes || !StringUtils.hasText(mailHost)) {
            log.info("[Почта] Код для {} (SMTP выключен или log-codes): {}", to, plainCode);
        }
        if (!StringUtils.hasText(mailHost) || mailSender == null) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        String sender = StringUtils.hasText(from) ? from : username;
        if (StringUtils.hasText(sender)) {
            message.setFrom(sender);
        }
        mailSender.send(message);
        log.info("[Почта] Письмо отправлено на {}", to);
    }
}

package org.example.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Slf4j
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${MAIL_HOST:${spring.mail.host:}}") String host,
            @Value("${MAIL_PORT:${spring.mail.port:465}}") int port,
            @Value("${MAIL_USERNAME:${spring.mail.username:}}") String username,
            @Value("${MAIL_PASSWORD:${spring.mail.password:}}") String password,
            @Value("${MAIL_SMTP_SSL:true}") boolean ssl
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        if (!StringUtils.hasText(host)) {
            log.warn("[Почта] MAIL_HOST пустой — SMTP не подключён");
            sender.setHost("localhost");
            sender.setPort(25);
            return sender;
        }
        log.info("[Почта] SMTP {}:{} user={} ssl={}", host.trim(), port, username, ssl);
        sender.setHost(host.trim());
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", Boolean.toString(ssl));
        props.put("mail.smtp.ssl.trust", host.trim());
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");
        return sender;
    }
}

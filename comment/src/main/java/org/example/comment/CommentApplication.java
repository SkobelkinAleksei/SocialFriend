package org.example.comment;

import com.example.common.kafka.NotificationKafkaProducer;
import org.example.restclient.config.RestClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import({RestClientConfig.class,
        NotificationKafkaProducer.class})
@SpringBootApplication
public class CommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentApplication.class, args);
    }

}

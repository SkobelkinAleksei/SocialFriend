package org.example.like;

import com.example.common.kafka.NotificationKafkaProducer;
import org.example.restclient.config.RestClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@Import({
        RestClientConfig.class,
        NotificationKafkaProducer.class
})
@SpringBootApplication
public class LikeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LikeApplication.class, args);
    }

}

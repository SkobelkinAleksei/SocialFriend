package org.example.notification.config;

import com.example.common.kafka.NotificationEvent;
import com.example.common.kafka.UserRegisteredEvent;
import com.example.common.kafka.UserSettingsUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private <T> ConsumerFactory<String, T> consumerFactory(Class<T> targetType) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetType);
        deserializer.addTrustedPackages("com.example.common.kafka");
        deserializer.ignoreTypeHeaders();
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), deserializer);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> containerFactory(Class<T> targetType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory(targetType));
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 3L)));
        return factory;
    }

    /** Основной inbox уведомлений */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> kafkaListenerContainerFactory() {
        return containerFactory(NotificationEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserSettingsUpdatedEvent> settingsKafkaListenerContainerFactory() {
        return containerFactory(UserSettingsUpdatedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent> registrationKafkaListenerContainerFactory() {
        return containerFactory(UserRegisteredEvent.class);
    }
}

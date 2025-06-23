package com.pentryyy.fragmented_file_transfer_api.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Consumer для Chunk
    @Bean
    public ConsumerFactory<String, Chunk> chunkConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "file-transfer-group");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        // Используем ErrorHandlingDeserializer для десериализации с обработкой ошибок
        return new DefaultKafkaConsumerFactory<>(
            config,
            new ErrorHandlingDeserializer<>(new StringDeserializer()),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(Chunk.class))
        );
    }

    // Consumer для Feedback
    @Bean
    public ConsumerFactory<String, Feedback> feedbackConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "file-feedback-group");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        // Используем ErrorHandlingDeserializer для десериализации с обработкой ошибок
        return new DefaultKafkaConsumerFactory<>(
            config,
            new ErrorHandlingDeserializer<>(new StringDeserializer()),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(Feedback.class))
        );
    }

    // Настройка обработчика ошибок для Feedback
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Chunk> chunkListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Chunk> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chunkConsumerFactory());
        
        // Настраиваем обработчик ошибок, 3 попытки с интервалом 1 сек
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (record, exception) -> {
                System.err.println("Ошибка обработки сообщения: " + record);
                exception.printStackTrace();
            },
            new FixedBackOff(1000, 3)
        );
        
        // Пропускаем ошибки десериализации
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        factory.setCommonErrorHandler(errorHandler);
        
        return factory;
    }

    // Настройка обработчика ошибок для Chunk
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Feedback> feedbackListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Feedback> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(feedbackConsumerFactory());
        
        // Настраиваем обработчик ошибок, 3 попытки с интервалом 1 сек
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (record, exception) -> {
                System.err.println("Ошибка обработки фидбэка: " + record);
                exception.printStackTrace();
            },
            new FixedBackOff(1000, 3)
        );

        // Пропускаем ошибки десериализации
        errorHandler.addNotRetryableExceptions(SerializationException.class);
        factory.setCommonErrorHandler(errorHandler);
        
        return factory;
    }

    // Producer Factories
    @Bean
    public ProducerFactory<String, Chunk> chunkProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ProducerFactory<String, Feedback> feedbackProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    // Kafka Templates
    @Bean
    public KafkaTemplate<String, Chunk> chunkKafkaTemplate() {
        return new KafkaTemplate<>(chunkProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, Feedback> feedbackKafkaTemplate() {
        return new KafkaTemplate<>(feedbackProducerFactory());
    }

    // Kafka Admin (для создания топиков)
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic chunksTopic() {
        return new NewTopic("file-chunks", 3, (short) 1);
    }

    @Bean
    public NewTopic feedbacksTopic() {
        return new NewTopic("file-feedbacks", 3, (short) 1);
    }
}

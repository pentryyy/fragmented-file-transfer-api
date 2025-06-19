package com.pentryyy.fragmented_file_transfer_api.service.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaFileProcessingProducerService {
    private static final String TOPIC_NAME = "file-processing-topic";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaFileProcessingProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String message) {
        kafkaTemplate.send(TOPIC_NAME, message);
    }
}

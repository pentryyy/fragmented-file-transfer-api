package com.pentryyy.fragmented_file_transfer_api.service.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaFileProcessingConsumerService {
    private static final String TOPIC_NAME = "file-processing-topic";
    private final Set<String> messages = ConcurrentHashMap.newKeySet();

    @KafkaListener(
        topics = TOPIC_NAME, 
        groupId = "file-group"
    )
    public void listen(String message) {       
        messages.add(message);
    }

    public List<String> getAllMessages() {
        return new ArrayList<>(messages);
    }
}

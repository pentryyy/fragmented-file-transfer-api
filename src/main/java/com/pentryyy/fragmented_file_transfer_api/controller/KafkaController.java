package com.pentryyy.fragmented_file_transfer_api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pentryyy.fragmented_file_transfer_api.service.kafka.KafkaFileProcessingConsumerService;
import com.pentryyy.fragmented_file_transfer_api.service.kafka.KafkaFileProcessingProducerService;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private final KafkaFileProcessingProducerService producerService;
    private final KafkaFileProcessingConsumerService consumerService;

    public KafkaController(
        KafkaFileProcessingProducerService producerService, 
        KafkaFileProcessingConsumerService consumerService
    ) {
        this.producerService = producerService;
        this.consumerService = consumerService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendToKafka() {
        String message = UUID.randomUUID().toString();
        producerService.sendMessage(message);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(null);
    }

    @GetMapping("/messages")
    public List<String> getAllMessages() {
        return consumerService.getAllMessages();
    }
}

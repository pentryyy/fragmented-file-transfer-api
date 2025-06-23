package com.pentryyy.fragmented_file_transfer_api.component;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;

@Component
public class KafkaTransmissionChannel {

    private final KafkaTemplate<String, Chunk>    chunkKafkaTemplate;
    private final KafkaTemplate<String, Feedback> feedbackKafkaTemplate;

    public KafkaTransmissionChannel(
        KafkaTemplate<String, Chunk> chunkKafkaTemplate,
        KafkaTemplate<String, Feedback> feedbackKafkaTemplate
    ) {
        this.chunkKafkaTemplate    = chunkKafkaTemplate;
        this.feedbackKafkaTemplate = feedbackKafkaTemplate;
    }

    public void sendChunk(Chunk chunk) {
        chunkKafkaTemplate.send(
            "file-chunks",
            chunk.getProcessingId(),
            chunk
        );
    }

    public void sendFeedback(Feedback feedback) {
        feedbackKafkaTemplate.send(
            "file-feedbacks", 
            feedback.getProcessingId(), 
            feedback
        );
    }
}

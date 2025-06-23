package com.pentryyy.fragmented_file_transfer_api.service.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;

@Service
public class FeedbackReceiver {

    @Autowired
    private FileSplitterManager splitterManager;

    @KafkaListener(
        topics = "file-feedbacks", 
        containerFactory = "feedbackListenerContainerFactory"
    )
    public void receiveFeedback(Feedback feedback) {
        splitterManager.getSplitter(feedback.getProcessingId()).receiveFeedback(feedback);
    }
}

package com.pentryyy.fragmented_file_transfer_api.service.kafka;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.pentryyy.fragmented_file_transfer_api.component.KafkaTransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;

@Service
public class FileSplitterManager {

    private final Map<String, FileSplitter> splitters = new ConcurrentHashMap<>();

    public FileSplitter createSplitter(
        String processingId, 
        int totalChunks, 
        KafkaTransmissionChannel channel
    ) {
        FileSplitter splitter = new FileSplitter(
            processingId, 
            totalChunks, 
            channel
        );
        
        splitters.put(processingId, splitter);
        return splitter;
    }

    public FileSplitter getSplitter(String processingId) {
        return splitters.get(processingId);
    }

    public void removeSplitter(String processingId) {
        splitters.remove(processingId);
    }
}

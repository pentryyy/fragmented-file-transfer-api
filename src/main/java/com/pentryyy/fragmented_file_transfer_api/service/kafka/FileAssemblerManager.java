package com.pentryyy.fragmented_file_transfer_api.service.kafka;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.pentryyy.fragmented_file_transfer_api.component.KafkaTransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;

@Service
public class FileAssemblerManager {

    private final Map<String, FileAssembler> assemblers = new ConcurrentHashMap<>();
    private final KafkaTransmissionChannel   channel;

    public FileAssemblerManager(KafkaTransmissionChannel channel) {
        this.channel = channel;
    }

    public FileAssembler getAssembler(String processingId) {
        return assemblers.computeIfAbsent(
            processingId, 
            id -> new FileAssembler(id, channel)
        );
    }

    public void removeAssembler(String processingId) {
        assemblers.remove(processingId);
    }
}

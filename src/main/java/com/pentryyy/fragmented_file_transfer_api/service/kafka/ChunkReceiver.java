package com.pentryyy.fragmented_file_transfer_api.service.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;

@Service
public class ChunkReceiver {

    @Autowired
    private FileAssemblerManager assemblerManager;

    @KafkaListener(
        topics = "file-chunks", 
        containerFactory = "chunkListenerContainerFactory"
    )
    public void receiveChunk(Chunk chunk) {
        assemblerManager.getAssembler(chunk.getProcessingId()).receiveChunk(chunk);
    }
}

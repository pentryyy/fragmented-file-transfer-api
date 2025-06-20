package com.pentryyy.fragmented_file_transfer_api.transfer.receiver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.pentryyy.fragmented_file_transfer_api.exception.ChunkIsMissingException;
import com.pentryyy.fragmented_file_transfer_api.exception.FileIncompleteException;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.TransmissionChannel;

public class FileAssembler {

    private final String              processingId;
    private final Map<Integer, Chunk> receivedChunks = new ConcurrentHashMap<>();
    private final TransmissionChannel channel;

    private volatile int totalChunks = -1;

    public FileAssembler(String processingId, TransmissionChannel channel) {
        this.processingId = processingId;
        this.channel      = channel;
    }

    public void receiveChunk(Chunk chunk) {
        if (!chunk.getProcessingId().equals(processingId)) 
            return;
        
        if (chunk.getSequenceNumber() == -1) {

            // Получаем мета чанк
            try {
                totalChunks = Integer.parseInt(new String(chunk.getData()));
                System.out.println("Received metadata (id " + this.processingId + "). Total chunks: " + totalChunks);
            } catch (NumberFormatException e) {
                System.err.println("Invalid metadata (id " + this.processingId + ") format: " + new String(chunk.getData()));
            }
            return;
        }
        
        receivedChunks.put(chunk.getSequenceNumber(), chunk);
    }

    public void sendFeedback() {
        if (totalChunks == -1) {

            // Запрос метаданных
            channel.sendFeedback(new Feedback(
                processingId, 
                -1, 
                Collections.singleton(-1)
            ));
            return;
        }
        
        // Проверка на наличие потерянных чанков
        Set<Integer> missing = new HashSet<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.containsKey(i)) {
                missing.add(i);
            }
        }
        
        if (!missing.isEmpty()) {
            System.out.println("Sending feedback (id " + this.processingId + "). Missing chunks: " + missing.size());
            channel.sendFeedback(new Feedback(processingId, totalChunks, missing));
        } else {
            System.out.println("Sending feedback (id " + this.processingId + "). Chunks delivered");
        }
    }

    public boolean isFileComplete() {
        return totalChunks > 0 && receivedChunks.size() >= totalChunks;
    }

    public void assembleFile(String outputPath) throws IOException {
        int missingCount = totalChunks > 0 ? totalChunks - receivedChunks.size() : -1;

        if (!isFileComplete()) {
            throw new FileIncompleteException(missingCount);
        }
        
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (int i = 0; i < totalChunks; i++) {
                Chunk chunk = receivedChunks.get(i);
                if (chunk == null) {
                    throw new ChunkIsMissingException(i);
                }
                fos.write(chunk.getData());
            }
        }
        System.out.println("File assembled successfully: " + outputPath);
    }
}

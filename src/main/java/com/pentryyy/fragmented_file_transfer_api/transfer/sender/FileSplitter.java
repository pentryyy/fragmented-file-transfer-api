package com.pentryyy.fragmented_file_transfer_api.transfer.sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.pentryyy.fragmented_file_transfer_api.component.KafkaTransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;

public class FileSplitter {

    private static final int MAX_RETRIES   = 5;
    private static final int META_SEQUENCE = -1;

    private final String                   processingId;
    private final KafkaTransmissionChannel channel;
    private final int                      totalChunks;

    private final Map<Integer, Chunk> chunks = new ConcurrentHashMap<>();
    private final Set<Integer> pendingChunks = ConcurrentHashMap.newKeySet();

    private final Map<Integer, AtomicInteger> retryCounters = new ConcurrentHashMap<>();
    private final Set<Integer>                failedChunks  = ConcurrentHashMap.newKeySet();

    private volatile boolean deliveryComplete;

    public FileSplitter(String processingId, int totalChunks, KafkaTransmissionChannel channel) {
        this.processingId = processingId;
        this.totalChunks  = totalChunks;
        this.channel      = channel;
    }

    public void splitFile(File file, int chunkSize) throws IOException {

        // Отправка метаданных (общее количество фрагментов)
        byte[] metaData  = String.valueOf(totalChunks).getBytes(StandardCharsets.UTF_8);
        Chunk  metaChunk = new Chunk(META_SEQUENCE, metaData, processingId);
        chunks.put(META_SEQUENCE, metaChunk);
        pendingChunks.add(META_SEQUENCE);
        channel.sendChunk(metaChunk);

        // Отправка основных фрагментов
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int sequence = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {

                // Отправляем данные чанков
                Chunk chunk = new Chunk(
                    sequence, 
                    Arrays.copyOf(buffer, bytesRead), 
                    processingId
                );
                chunks.put(sequence, chunk);
                pendingChunks.add(sequence);
                channel.sendChunk(chunk);

                sequence++;
            }
        }
    }

    public void receiveFeedback(Feedback feedback) {
        if (!feedback.getProcessingId().equals(processingId)) 
            return;
        
        Set<Integer> missing = feedback.getMissingSequences();
        if (missing.isEmpty()) {
            deliveryComplete = true;
            return;
        }
        
        // Считаем потерянные чанки
        Set<Integer> newPending = ConcurrentHashMap.newKeySet();
        
        for (int seq : missing) {
            if (failedChunks.contains(seq))
                continue;
            
            int attempts = retryCounters
                .computeIfAbsent(seq, k -> new AtomicInteger(0))
                .incrementAndGet();
            
            if (attempts > MAX_RETRIES) {
                failedChunks.add(seq);
            } else {
                newPending.add(seq);
            }
        }
        
        pendingChunks.clear();
        pendingChunks.addAll(newPending);

        // Отправляем требуемый чанк
        for (int seq : pendingChunks) {
            Chunk chunk = chunks.get(seq);
            if (chunk != null) {
                channel.sendChunk(chunk);
            }
        }

        if (!failedChunks.isEmpty()) {
            deliveryComplete = false;
        }
    }

    public boolean isDeliveryComplete() {
        return deliveryComplete;
    }

    public Set<Integer> getFailedChunks() {
        return Collections.unmodifiableSet(failedChunks);
    }
}

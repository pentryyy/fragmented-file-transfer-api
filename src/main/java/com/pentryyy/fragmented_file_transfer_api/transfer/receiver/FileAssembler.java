package com.pentryyy.fragmented_file_transfer_api.transfer.receiver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pentryyy.fragmented_file_transfer_api.component.KafkaTransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.exception.ChunkIsMissingException;
import com.pentryyy.fragmented_file_transfer_api.exception.FileIncompleteException;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;

public class FileAssembler {

    private final String                   processingId;
    private final KafkaTransmissionChannel channel;
    
    private final Map<Integer, Chunk> receivedChunks = new ConcurrentHashMap<>();
    private final AtomicBoolean       isAssembling   = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;

    private volatile int totalChunks = -1;

    private void sendFeedback() {
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

    private boolean isFileComplete() {
        return totalChunks > 0 && receivedChunks.size() >= totalChunks;
    }

    private void startFeedbackScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        // Периодически отправляем запросы на недостающие чанки
        scheduler.scheduleAtFixedRate(() -> {
            if (!isFileComplete()) {
                sendFeedback();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void stopFeedbackScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.err.println("Scheduler did not terminate");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
        
        // Проверяем каждую секунду
        while (!isFileComplete() && System.currentTimeMillis() < endTime) {
            Thread.sleep(1000);
        }
    }

    private void writeFile(String outputPath) throws IOException {
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

    public FileAssembler(String processingId, KafkaTransmissionChannel channel) {
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

    public void assembleFile(String outputPath) throws IOException, InterruptedException {
        
        if (!isAssembling.compareAndSet(false, true)) {
            throw new IllegalStateException("Assembly is already in progress");
        }

        try {
            // 1. Запускаем периодическую проверку готовности
            startFeedbackScheduler();
            
            // 2. Ожидаем завершения сборки с таймаутом
            waitForCompletion(5, TimeUnit.MINUTES);
            
            // 3. Проверяем статус и собираем файл
            if (isFileComplete()) {
                writeFile(outputPath);
            } else {
                throw new FileIncompleteException(totalChunks - receivedChunks.size());
            }
        } finally {
            stopFeedbackScheduler();
            isAssembling.set(false);
        }
    }
}

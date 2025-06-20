package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;

public class TransmissionChannel {

    private final double lossProbability;
    private final Random random = new Random();

    private FileAssembler assembler;
    private FileSplitter  splitter;

    private ScheduledExecutorService feedbackScheduler;

    private void startFeedbackScheduler() {
        feedbackScheduler = Executors.newSingleThreadScheduledExecutor();
        feedbackScheduler.scheduleAtFixedRate(() -> {

            if (assembler == null) 
                return;

            if (assembler.isFileComplete()) {
                assembler.sendFeedback();

                // Останавливаем планировщик
                if (feedbackScheduler != null) {
                    feedbackScheduler.shutdown();
                    try {
                        if (!feedbackScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                            feedbackScheduler.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        feedbackScheduler.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }
                return;
            }
            assembler.sendFeedback();
        }, 0, 1, TimeUnit.SECONDS);
    }

    public TransmissionChannel(double lossProbability) {
        this.lossProbability = lossProbability;
        startFeedbackScheduler();
    }

    public void registerReceiver(FileAssembler assembler) {
        this.assembler = assembler;
    }

    public void registerReceiver(FileSplitter splitter) {
        this.splitter = splitter;
    }

    public void sendChunk(Chunk chunk) {
        if (random.nextDouble() >= lossProbability)
            assembler.receiveChunk(chunk);
    }

    public void sendFeedback(Feedback feedback) {
        splitter.receiveFeedback(feedback);
    }
}

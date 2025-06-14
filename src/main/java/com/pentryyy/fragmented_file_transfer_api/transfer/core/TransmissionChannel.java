package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.util.Random;

import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;

public class TransmissionChannel {
    private final double lossProbability;
    private final Random random = new Random();
    private FileAssembler assembler;
    private FileSplitter splitter;

    public TransmissionChannel(double lossProbability) {
        this.lossProbability = lossProbability;
    }

    public void registerReceiver(FileAssembler assembler) {
        this.assembler = assembler;
    }

    public void registerReceiver(FileSplitter splitter) {
        this.splitter = splitter;
    }

    public void sendChunk(Chunk chunk) {
        if (assembler == null) throw new IllegalStateException("FileAssembler not registered");
        if (random.nextDouble() >= lossProbability) {
            assembler.receiveChunk(chunk);
        }
    }

    public void sendFeedback(Feedback feedback) {
        if (splitter == null) throw new IllegalStateException("FileSplitter not registered");
        splitter.receiveFeedback(feedback);
    }
}

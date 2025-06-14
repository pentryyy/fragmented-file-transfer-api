package com.pentryyy.fragmented_file_transfer_api.transfer.receiver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.TransmissionChannel;

public class FileAssembler {
    private final int fileId;
    private final int totalChunks;
    private final Map<Integer, Chunk> receivedChunks = new ConcurrentHashMap<>();
    private final TransmissionChannel channel;

    public FileAssembler(int fileId, int totalChunks, TransmissionChannel channel) {
        this.fileId = fileId;
        this.totalChunks = totalChunks;
        this.channel = channel;
    }

    public void receiveChunk(Chunk chunk) {
        if (chunk.getFileId() != fileId) return;
        receivedChunks.put(chunk.getSequenceNumber(), chunk);
    }

    public void sendFeedback() {
        Set<Integer> missing = new HashSet<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.containsKey(i)) missing.add(i);
        }
        channel.sendFeedback(new Feedback(fileId, missing));
    }

    public boolean isFileComplete() {
        return receivedChunks.size() == totalChunks;
    }

    public void assembleFile(String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (int i = 0; i < totalChunks; i++) {
                Chunk chunk = receivedChunks.get(i);
                if (chunk != null) {
                    fos.write(chunk.getData());
                }
            }
        }
    }
}

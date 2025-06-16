package com.pentryyy.fragmented_file_transfer_api.transfer.sender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.pentryyy.fragmented_file_transfer_api.transfer.core.Chunk;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.Feedback;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.TransmissionChannel;

public class FileSplitter {

    private final String              processingId;
    private final TransmissionChannel channel;

    private final Map<Integer, Chunk> chunks        = new ConcurrentHashMap<>();
    private final Set<Integer>        pendingChunks = ConcurrentHashMap.newKeySet();

    private volatile boolean deliveryComplete = false;

    public FileSplitter(String processingId, TransmissionChannel channel) {
        this.processingId  = processingId;
        this.channel = channel;
    }

    public void splitFile(File file, int chunkSize) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            int sequence = 0;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunkData = Arrays.copyOf(buffer, bytesRead);
                Chunk chunk = new Chunk(sequence, chunkData, processingId);
                chunks.put(sequence, chunk);
                pendingChunks.add(sequence);
                channel.sendChunk(chunk);
                sequence++;
            }
        }
    }

    public void receiveFeedback(Feedback feedback) {
        if (feedback.getProcessingId() != processingId) return;
        
        Set<Integer> missing = feedback.getMissingSequences();
        if (missing.isEmpty()) {
            deliveryComplete = true;
            return;
        }
        
        pendingChunks.addAll(missing);
        resendChunks(missing);
    }

    private void resendChunks(Set<Integer> sequences) {
        for (int seq : sequences) {
            Chunk chunk = chunks.get(seq);
            if (chunk != null) {
                channel.sendChunk(chunk);
            }
        }
    }

    public boolean isDeliveryComplete() { return deliveryComplete; }
    public Set<Integer> getPendingChunks() { return Collections.unmodifiableSet(pendingChunks); }
}

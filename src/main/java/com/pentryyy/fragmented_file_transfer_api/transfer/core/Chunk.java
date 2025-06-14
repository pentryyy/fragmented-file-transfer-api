package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.io.Serializable;

public class Chunk implements Serializable {
    private final int sequenceNumber;
    private final byte[] data;
    private final int fileId;

    public Chunk(int fileId, int sequenceNumber, byte[] data) {
        this.fileId = fileId;
        this.sequenceNumber = sequenceNumber;
        this.data = data;
    }

    public int getSequenceNumber() { return sequenceNumber; }
    public byte[] getData() { return data; }
    public int getFileId() { return fileId; }
}

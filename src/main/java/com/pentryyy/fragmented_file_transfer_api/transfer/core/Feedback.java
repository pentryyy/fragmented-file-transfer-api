package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.io.Serializable;
import java.util.Set;

public class Feedback implements Serializable {
    private final int fileId;
    private final Set<Integer> missingSequences;

    public Feedback(int fileId, Set<Integer> missingSequences) {
        this.fileId = fileId;
        this.missingSequences = missingSequences;
    }

    public int getFileId() { return fileId; }
    public Set<Integer> getMissingSequences() { return missingSequences; }
}

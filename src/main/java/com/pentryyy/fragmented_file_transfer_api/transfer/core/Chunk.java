package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Chunk implements Serializable {
    private final int    sequenceNumber;
    private final byte[] data;
    private final String processingId;
}

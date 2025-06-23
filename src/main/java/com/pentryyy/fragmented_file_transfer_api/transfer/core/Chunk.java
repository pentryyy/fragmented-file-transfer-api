package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor 
@AllArgsConstructor
public class Chunk implements Serializable {
    private int    sequenceNumber;
    private byte[] data;
    private String processingId;
}

package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.io.Serializable;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor 
@AllArgsConstructor
public class Feedback implements Serializable {
    private String       processingId;
    private int          totalChunks;
    private Set<Integer> missingSequences;
}

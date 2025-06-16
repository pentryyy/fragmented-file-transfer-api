package com.pentryyy.fragmented_file_transfer_api.transfer.core;

import java.io.Serializable;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Feedback implements Serializable {
    private final String       processingId;
    private final Set<Integer> missingSequences;
}

package com.pentryyy.fragmented_file_transfer_api.model;

import java.io.File;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FileTask {
    @EqualsAndHashCode.Include
    private final String processingId;
    
    private FileTaskStatus status;
    private int            chunkSize;
    private double         lossProbability;
    private LocalDateTime  timestamp;

    @JsonIgnore
    private File file;
}
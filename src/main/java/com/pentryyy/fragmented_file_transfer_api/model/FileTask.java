package com.pentryyy.fragmented_file_transfer_api.model;

import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FileTask {
    @EqualsAndHashCode.Include
    private final String processingId;
    
    private FileTaskStatus status;
    private String         currentOutputFilePath;
}

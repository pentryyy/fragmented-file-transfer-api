package com.pentryyy.fragmented_file_transfer_api.model;

import java.util.HashSet;
import java.util.Set;

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
    private String         outputFileName;
    private Set<Integer>   missingFragments;

    public FileTask(String processingId, FileTaskStatus status, String outputFileName) {
        this.processingId   = processingId;
        this.status         = status;
        this.outputFileName = outputFileName;
        this.missingFragments = new HashSet<>();
    }
}

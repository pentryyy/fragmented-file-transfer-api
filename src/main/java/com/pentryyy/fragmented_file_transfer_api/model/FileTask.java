package com.pentryyy.fragmented_file_transfer_api.model;

import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileTask {
    private String         processingId;
    private FileTaskStatus status;
}

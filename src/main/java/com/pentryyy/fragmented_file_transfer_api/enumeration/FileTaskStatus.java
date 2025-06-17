package com.pentryyy.fragmented_file_transfer_api.enumeration;

public enum FileTaskStatus {
    CREATED,
    PROCESS_INTERRUPTED,
    SPLIT_PROCESSING,
    SPLIT_FAILED,
    SPLIT_COMPLETED,
    ASSEMBLE_PROCESSING,
    ASSEMBLE_FAILED,
    ASSEMBLE_COMPLETED
}

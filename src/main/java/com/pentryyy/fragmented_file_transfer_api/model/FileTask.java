package com.pentryyy.fragmented_file_transfer_api.model;

import java.time.LocalDateTime;
import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "log_of_processes")
public class FileTask {

    @Id
    @Column(name = "processing_id", length = 36, nullable = false)
    private String processingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FileTaskStatus status;

    @Column(name = "chunk_size", nullable = false)
    private int chunkSize;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}

package com.pentryyy.fragmented_file_transfer_api.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;
import com.pentryyy.fragmented_file_transfer_api.model.FileTask;

@Repository
public interface LogOfProcessRepository extends JpaRepository<FileTask, String> {

    @Query("SELECT f.status FROM FileTask f WHERE f.processingId = :id")
    Optional<FileTaskStatus> findStatusById(@Param("id") String processingId);

    @Modifying
    @Query("UPDATE FileTask f SET f.status = :status WHERE f.processingId = :id")
    void updateStatusById(
        @Param("id") String processingId, 
        @Param("status") FileTaskStatus status
    );

    @Modifying
    @Query("UPDATE FileTask f SET f.timestamp = :timestamp WHERE f.processingId = :id")
    void updateTimestampById(
        @Param("id") String processingId, 
        @Param("timestamp") LocalDateTime timestamp
    );
}

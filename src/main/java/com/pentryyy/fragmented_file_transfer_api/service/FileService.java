package com.pentryyy.fragmented_file_transfer_api.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pentryyy.fragmented_file_transfer_api.component.KafkaTransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;
import com.pentryyy.fragmented_file_transfer_api.exception.FileNotAssembledException;
import com.pentryyy.fragmented_file_transfer_api.exception.FileNotSplitedException;
import com.pentryyy.fragmented_file_transfer_api.exception.FileProcessNotFoundException;
import com.pentryyy.fragmented_file_transfer_api.model.FileTask;
import com.pentryyy.fragmented_file_transfer_api.repository.LogOfProcessRepository;
import com.pentryyy.fragmented_file_transfer_api.service.kafka.FileAssemblerManager;
import com.pentryyy.fragmented_file_transfer_api.service.kafka.FileSplitterManager;
import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;
import com.pentryyy.fragmented_file_transfer_api.utils.DirectoryUtils;

@Service
public class FileService {

    private File tempFile;

    @Autowired
    private LogOfProcessRepository logOfProcessRepository;

    @Autowired
    private KafkaTransmissionChannel channel;

    @Autowired
    private FileSplitterManager splitterManager;

    @Autowired
    private FileAssemblerManager assemblerManager;

    private FileTask findFileTaskById(String processingId) {
        return logOfProcessRepository
            .findById(processingId)
            .orElseThrow(() -> new FileProcessNotFoundException(processingId));
    }

    public String initializingFileProcessing(
        MultipartFile file, 
        int chunkSize
    ) throws IOException {

        // Создаем уникальный ID для обработки
        String processingId = UUID.randomUUID().toString();

        // Создаем директории для обработки
        Files.createDirectories(Paths.get(
            DirectoryUtils.getOutputDir(processingId)
        ));

        FileTask fileTask = FileTask
            .builder()
            .processingId(processingId)
            .status(FileTaskStatus.CREATED)
            .chunkSize(chunkSize)
            .timestamp(LocalDateTime.now())
            .build();

        this.tempFile = DirectoryUtils.convert(file);

        logOfProcessRepository.save(fileTask);

        return processingId;
    }

    public FileTaskStatus getStatusById(String processingId) {
        return logOfProcessRepository
            .findStatusById(processingId)
            .orElseThrow(() -> new FileProcessNotFoundException(processingId));
    }

    public Page<FileTask> getAllTasks(
        int page, 
        int limit,
        String sortBy, 
        String sortOrder
    ) {
        Sort sort = sortOrder.equalsIgnoreCase(Sort.Direction.ASC.name())
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        return logOfProcessRepository.findAll(PageRequest.of(page, limit, sort));
    }

    @Transactional
    public void splittingFileIntoChunks(String processingId) {
        FileTask fileTask = findFileTaskById(processingId);
        
        try {
            logOfProcessRepository.updateStatusById(processingId, FileTaskStatus.SPLIT_PROCESSING);

            long fileSize    = this.tempFile.length();
            int  totalChunks = (int) Math.ceil((double) fileSize / fileTask.getChunkSize());

            FileSplitter splitter = splitterManager.createSplitter(
                processingId,
                totalChunks,
                channel
            );

            // Разбиение файла на чанки
            splitter.splitFile(
                this.tempFile,
                fileTask.getChunkSize()
            );

            logOfProcessRepository.updateStatusById(processingId, FileTaskStatus.SPLIT_COMPLETED);
            logOfProcessRepository.updateTimestampById(processingId, LocalDateTime.now());

        } catch (IOException e) {
            logOfProcessRepository.updateStatusById(processingId, FileTaskStatus.SPLIT_FAILED);
            throw new FileNotSplitedException();
        }
    }

    @Transactional
    public void assembleFileFromChunks(String processingId) {

        try {
            logOfProcessRepository.updateStatusById(processingId, FileTaskStatus.ASSEMBLE_PROCESSING);
            
            FileAssembler assembler = assemblerManager.getAssembler(processingId);

            // Сборка файла
            assembler.assembleFile(
                DirectoryUtils.getOutputDir(processingId) + "assembled_" + this.tempFile.getName()
            );
            assemblerManager.removeAssembler(processingId);

            logOfProcessRepository.updateStatusById(processingId, FileTaskStatus.ASSEMBLE_COMPLETED);
            logOfProcessRepository.updateTimestampById(processingId, LocalDateTime.now());
        
        } catch (IOException | InterruptedException e) {
            logOfProcessRepository.updateStatusById(processingId, FileTaskStatus.ASSEMBLE_FAILED);
            throw new FileNotAssembledException();
        }
    }
}

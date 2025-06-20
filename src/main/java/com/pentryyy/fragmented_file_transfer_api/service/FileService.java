package com.pentryyy.fragmented_file_transfer_api.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;
import com.pentryyy.fragmented_file_transfer_api.exception.FileNotAssembledException;
import com.pentryyy.fragmented_file_transfer_api.exception.FileNotSplitedException;
import com.pentryyy.fragmented_file_transfer_api.exception.FileProcessNotFoundException;
import com.pentryyy.fragmented_file_transfer_api.model.FileTask;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.TransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;
import com.pentryyy.fragmented_file_transfer_api.utils.DirectoryUtils;

@Service
public class FileService {

    private static Set<FileTask> collectionOfProcesses = ConcurrentHashMap.newKeySet();
    
    private FileTask fileTask;

    private TransmissionChannel channel;
    private FileSplitter        splitter;
    private FileAssembler       assembler;

    public FileTask findFileTaskById(String processingId) {
        return collectionOfProcesses
            .stream()
            .filter(task -> Objects.equals(task.getProcessingId(), processingId))
            .findFirst()
            .orElseThrow(() -> new FileProcessNotFoundException(processingId));
    }

    public String initializingFileProcessing(
        MultipartFile file, 
        double lossProbability,
        int chunkSize
    ) throws IOException {

        // Создаем уникальный ID для обработки
        String processingId = UUID.randomUUID().toString();

        // Создаем директории для обработки
        Files.createDirectories(Paths.get(
            DirectoryUtils.getOutputDir(processingId)
        ));

        this.fileTask = FileTask
            .builder()
            .processingId(processingId)
            .status(FileTaskStatus.CREATED)
            .chunkSize(chunkSize)
            .lossProbability(lossProbability)
            .timestamp(LocalDateTime.now())
            .file(DirectoryUtils.convert(file))
            .build();

        collectionOfProcesses.add(fileTask);

        return processingId;
    }

    public void setupConfigureProcessing(String processingId) {
        FileTask fileTask = findFileTaskById(processingId);

        this.channel = new TransmissionChannel(fileTask.getLossProbability());

        this.splitter = new FileSplitter(
            fileTask.getProcessingId(), 
            this.channel
        );

        long fileSize    = fileTask.getFile().length();
        int  totalChunks = (int) Math.ceil((double) fileSize / fileTask.getChunkSize());
        
        this.assembler = new FileAssembler(
            processingId,
            totalChunks,
            this.channel
        );

        this.channel.registerReceiver(splitter);
        this.channel.registerReceiver(assembler);
    }

    public FileTaskStatus getStatusById(String processingId) {
        return collectionOfProcesses
            .stream()
            .filter(task -> task.getProcessingId().equals(processingId))
            .findFirst()
            .map(FileTask::getStatus)
            .orElseThrow(() -> new FileProcessNotFoundException(processingId));
    }

    public Page<FileTask> getAllTasks(
        int page,
        int limit,
        String sortBy,
        Sort.Direction sortOrder
    ) {

        List<FileTask> tasks = new ArrayList<>(collectionOfProcesses);

        int totalItems = tasks.size();
        
        int start = Math.min(page * limit, totalItems);
        int end   = Math.min(start + limit, totalItems);
        
        // Получаем подсписок для текущей страницы
        List<FileTask> pageContent = tasks.subList(start, end);
        
        // Создаем объект страницы
        PageRequest pageable = PageRequest.of(
            page, 
            limit, 
            Sort.by(sortOrder, sortBy)
        );
        
        return new PageImpl<>(pageContent, pageable, totalItems);
    }

    public File getFileById(String processingId) throws FileNotFoundException {
        FileTask fileTask = findFileTaskById(processingId);

        String filePath = DirectoryUtils.getOutputDir(processingId) + "assembled_" + fileTask.getFile().getName();
        File   file     = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException(
                "Файл не найден по адресу: " + filePath
            );
        }

        return file;
    }

    public void splittingFileIntoChunks(String processingId) {
        FileTask fileTask = findFileTaskById(processingId);
        
        try {
            fileTask.setStatus(FileTaskStatus.SPLIT_PROCESSING);

            // Разбиение файла на чанки
            this.splitter.splitFile(
                fileTask.getFile(),
                fileTask.getChunkSize()
            );

            // Проверка на то, доставлены ли все чанки
            while (!this.splitter.isDeliveryComplete()) {
                this.assembler.sendFeedback();
            }

            fileTask.setStatus(FileTaskStatus.SPLIT_COMPLETED);
            fileTask.setTimestamp(LocalDateTime.now());

        } catch (Exception e) {
            fileTask.setStatus(FileTaskStatus.SPLIT_FAILED);
            throw new FileNotSplitedException();
        }
    }

    public void assembleFileFromChunks(String processingId) {
        FileTask fileTask = findFileTaskById(processingId);

        try {
            fileTask.setStatus(FileTaskStatus.ASSEMBLE_PROCESSING);

            // Проверка на то, получены ли все чанки файла
            while (!this.assembler.isFileComplete()) {
                this.assembler.sendFeedback();
            }
            
            // Сборка файла
            this.assembler.assembleFile(
                DirectoryUtils.getOutputDir(processingId) + "assembled_" + fileTask.getFile().getName()
            );

            fileTask.setStatus(FileTaskStatus.ASSEMBLE_COMPLETED);
            fileTask.setTimestamp(LocalDateTime.now());
        
        } catch (IOException e) {
            fileTask.setStatus(FileTaskStatus.ASSEMBLE_FAILED);
            throw new FileNotAssembledException();
        }
    }
}

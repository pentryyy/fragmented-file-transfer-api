package com.pentryyy.fragmented_file_transfer_api.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;
import com.pentryyy.fragmented_file_transfer_api.enumeration.FileType;
import com.pentryyy.fragmented_file_transfer_api.exception.FileProcessNotFoundException;
import com.pentryyy.fragmented_file_transfer_api.model.FileTask;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.TransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;
import com.pentryyy.fragmented_file_transfer_api.utils.DirectoryUtils;

@Service
public class FileService extends DirectoryUtils {

    private static Set<FileTask> statusOfFiles = new HashSet<FileTask>();
    
    private FileTask            fileTask;
    private TransmissionChannel channel;
    private FileSplitter        splitter;
    private FileAssembler       assembler;

    private File inputFile;

    private FileTask findFileTaskById(String processingId) {
        synchronized (statusOfFiles) {
            return statusOfFiles
                .stream()
                .filter(task -> Objects.equals(task.getProcessingId(), processingId))
                .findFirst()
                .orElseThrow(() -> new FileProcessNotFoundException(processingId));
        }
    }

    public String initializingFileProcessing(
        MultipartFile file, 
        double lossProbability,
        int chunkSize
    ) throws IOException {

        // Создаем уникальный ID для обработки
        String processingId = UUID.randomUUID().toString();

        // Создаем директории для обработки
        String inputDir  = getInputDir(processingId);
        String outputDir = getOutputDir(processingId);

        Files.createDirectories(Paths.get(inputDir));
        Files.createDirectories(Paths.get(outputDir));

        // Сохраняем загруженный файл
        String originalFileName = file.getOriginalFilename();
        Path   inputFilePath    = Paths.get(inputDir + originalFileName);

        file.transferTo(inputFilePath);

        this.fileTask = FileTask
            .builder()
            .processingId(processingId)
            .status(FileTaskStatus.CREATED)
            .fileName(originalFileName)
            .chunkSize(chunkSize)
            .build();

        statusOfFiles.add(fileTask);

        // Конфигурация обработки
        this.inputFile = inputFilePath.toFile();

        this.channel  = new TransmissionChannel(lossProbability);
        this.splitter = new FileSplitter(processingId, channel);

        this.assembler = new FileAssembler(
            processingId, 
            (int) Math.ceil((double) inputFile.length() / chunkSize), 
            channel
        );

        channel.registerReceiver(assembler);
        channel.registerReceiver(splitter);

        return processingId;
    }

    public void processFileTask(String processingId) {
        ScheduledExecutorService scheduler = null;
        FileTask fileTask = findFileTaskById(processingId);

        try {
            scheduler = Executors.newScheduledThreadPool(1);
            
            // 1. Разбиваем файл на чанки
            splitter.splitFile(
                inputFile, 
                fileTask.getChunkSize()
            );
            
            // 2. Запускаем периодическую отправку фидбэка
            scheduler.scheduleAtFixedRate(() -> {
                if (!splitter.isDeliveryComplete()) {
                    assembler.sendFeedback();
                }
            }, 0, 1, TimeUnit.SECONDS);

            // 3. Ожидаем завершения доставки
            while (!splitter.isDeliveryComplete()) {
                Thread.sleep(500);
            }
            
            // 4. Останавливаем планировщик
            scheduler.shutdown();
            
            // 5. Собираем файл если все чанки получены
            if (assembler.isFileComplete()) {
                String outputPath = getOutputDir(processingId) + "assembled_" + fileTask.getFileName();
                assembler.assembleFile(outputPath);
            }

            // 6. Обновляем статус
            fileTask.setStatus(FileTaskStatus.COMPLETED);
        } catch (IOException | InterruptedException e) {
            fileTask.setStatus(FileTaskStatus.FAILED);
        } finally {
            // Гарантированное завершение планировщика
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
        }
    }

    public FileTaskStatus getStatusById(String processingId) {
        return statusOfFiles
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

        List<FileTask> tasks;
        synchronized (statusOfFiles) {
            tasks = new ArrayList<>(statusOfFiles);
        }

        int totalItems = tasks.size();
        int start = Math.min(page * limit, totalItems);
        int end = Math.min(start + limit, totalItems);
        
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

    public File getFileById(String processingId, FileType fileType) throws FileNotFoundException {
        FileTask fileTask = findFileTaskById(processingId);

        String filePath = "";
        File   file     = null;

        if (fileType.equals(FileType.OUTPUT)) {
            filePath = getOutputDir(processingId) + "assembled_" + fileTask.getFileName();
            file = new File(filePath);
        } else if (fileType.equals(FileType.INPUT)) {
            filePath = getInputDir(processingId) + fileTask.getFileName();
            file = new File(filePath);
        }

        if (!file.exists()) {
            throw new FileNotFoundException(
                "Файл не найден по адресу: " + filePath
            );
        }

        return file;
    }
}

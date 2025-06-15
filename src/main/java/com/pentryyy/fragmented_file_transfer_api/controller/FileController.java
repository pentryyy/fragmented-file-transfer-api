package com.pentryyy.fragmented_file_transfer_api.controller;

import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.pentryyy.fragmented_file_transfer_api.enumeration.FileTaskStatus;
import com.pentryyy.fragmented_file_transfer_api.exception.FileNotAvailableException;
import com.pentryyy.fragmented_file_transfer_api.exception.FileProcessNotFoundException;
import com.pentryyy.fragmented_file_transfer_api.model.FileTask;
import com.pentryyy.fragmented_file_transfer_api.transfer.core.TransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@Tag(name = "Управление файлами", description = "Операции для загрузки, обработки и скачивания файлов")
public class FileController {

    private static final String RESOURCES_DIR = "src/main/resources/";

    private static HashMap<String, FileTaskStatus> statusOfFiles = new HashMap<String, FileTaskStatus>();
    private        String                          currentOutputFilePath;

    @Operation(
        summary = "Загрузка и обработка файла",
        description = "Загружает файл на сервер, запускает процесс разделения на чанки и сборки с имитацией потери пакетов"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Файл успешно принят в обработку",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Ошибка при обработке файла",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/upload")
    public ResponseEntity<String> uploadAndProcessFile(
        @Parameter(
            description = "Файл для обработки",
            required = true,
            example = "document.pdf"
        ) 
        @RequestParam("file") MultipartFile file,

        @Parameter(
            description = "Вероятность потери пакетов (0.0-1.0)",
            example = "0.3"
        ) 
        @RequestParam(value = "lossProbability", defaultValue = "0.3") double lossProbability
    ) {
        
        JSONObject jsonObject = new JSONObject();

        try {
            // Создаем уникальный ID для обработки
            String processingId = UUID.randomUUID().toString();

            // Создаем директории для обработки
            String inputDir = RESOURCES_DIR + "input/" + processingId + "/";
            String outputDir = RESOURCES_DIR + "output/" + processingId + "/";

            Files.createDirectories(Paths.get(inputDir));
            Files.createDirectories(Paths.get(outputDir));

            // Сохраняем загруженный файл
            String originalFileName = file.getOriginalFilename();
            Path inputFilePath = Paths.get(inputDir + originalFileName);
            file.transferTo(inputFilePath);

            // Добавляем в хэш-таблицу
            statusOfFiles.put(processingId, FileTaskStatus.PROCESSING);

            // Готовим выходной файл
            this.currentOutputFilePath = outputDir + "assembled_" + originalFileName;
            File outputFile = new File(currentOutputFilePath);

            if (outputFile.exists()) 
                outputFile.delete();

            // Конфигурация обработки
            File inputFile = inputFilePath.toFile();

            int chunkSize = 1024;
            int fileId    = processingId.hashCode();

            TransmissionChannel channel = new TransmissionChannel(lossProbability);
            FileSplitter splitter = new FileSplitter(fileId, channel);
            int totalChunks = (int) Math.ceil((double) inputFile.length() / chunkSize);
            FileAssembler assembler = new FileAssembler(fileId, totalChunks, channel);

            channel.registerReceiver(assembler);
            channel.registerReceiver(splitter);

            // Запуск обработки в отдельном потоке
            new Thread(() -> {
                try {
                    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                    
                    splitter.splitFile(inputFile, chunkSize);
                    
                    scheduler.scheduleAtFixedRate(() -> {
                        if (!splitter.isDeliveryComplete()) {
                            assembler.sendFeedback();
                        }
                    }, 0, 1, TimeUnit.SECONDS);

                    while (!splitter.isDeliveryComplete()) {
                        Thread.sleep(500);
                    }
                    scheduler.shutdown();
                    
                    if (assembler.isFileComplete()) {
                        assembler.assembleFile(currentOutputFilePath);
                    }

                    statusOfFiles.replace(processingId, FileTaskStatus.COMPLETED);
                } catch (IOException | InterruptedException e) {
                    statusOfFiles.replace(processingId, FileTaskStatus.FAILED);
                }
            }).start();

            jsonObject.put("processingId", processingId);
            jsonObject.put("status", statusOfFiles.get(processingId));

            return ResponseEntity.ok()
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(jsonObject.toString());

        } catch (IOException e) {
            jsonObject.put("File processing error", e.getMessage());

            return ResponseEntity.internalServerError() 
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(jsonObject.toString());
        }
    }

    @Operation(
        summary = "Скачивание обработанного файла",
        description = "Возвращает собранный файл по идентификатору обработки"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Файл успешно доставлен",
            content = @Content(mediaType = "application/octet-stream")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Идентификатор обработки не найден"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Файл еще не доступен для скачивания"
        )
    })
    @GetMapping("/download/{processingId}")
    public ResponseEntity<Resource> downloadFile(
        @Parameter(
            description = "Уникальный идентификатор обработки",
            required = true,
            example = "d3b07384-1137-47ad-8bb4-0f1c1ffc5a1d"
        ) 
        @PathVariable String processingId
    ) {

        if (!statusOfFiles.containsKey(processingId)) {
            throw new FileProcessNotFoundException(processingId);
        }
        
        if (!statusOfFiles.get(processingId).equals(FileTaskStatus.COMPLETED)) {
            throw new FileNotAvailableException();
        }

        try {
            File file = new File(currentOutputFilePath);

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                                 .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                 .body(resource);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @Operation(
        summary = "Проверка статуса обработки",
        description = "Возвращает текущий статус обработки файла"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Статус обработки",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Идентификатор обработки не найден"
        )
    })
    @GetMapping("/status/{processingId}")
    public ResponseEntity<String> getStatus(
        @Parameter(
            description = "Уникальный идентификатор обработки",
            required = true,
            example = "d3b07384-1137-47ad-8bb4-0f1c1ffc5a1d"
        ) 
        @PathVariable String processingId
    ) {

        if (!statusOfFiles.containsKey(processingId)) {
            throw new FileProcessNotFoundException(processingId);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", statusOfFiles.get(processingId));
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }

    @Operation(
        summary = "Проверка статуса обработки",
        description = "Возвращает текущий статус обработки файла"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Статус обработки",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Идентификатор обработки не найден"
        )
    })
    @GetMapping("/get-all-tasks")
    public ResponseEntity<Page<FileTask>> getAllTasks(
        @Parameter(
            description = "Номер страницы (с 0)",
            example = "0"
        ) 
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(
            description = "Количество элементов на странице",
            example = "10"
        ) 
        @RequestParam(defaultValue = "10") int limit,
        
        @Parameter(
            description = "Поле для сортировки",
            schema = @Schema(allowableValues = {"processingId", "status"}),
            example = "processingId"
        ) 
        @RequestParam(defaultValue = "processingId") String sortBy,
        
        @Parameter(
            description = "Порядок сортировки",
            schema = @Schema(allowableValues = {"ASC", "DESC"}),
            example = "ASC"
        ) 
        @RequestParam(defaultValue = "ASC") Sort.Direction sortOrder
    ) {
        
        // 1. Преобразование HashMap в список FileTask
        List<FileTask> tasks = statusOfFiles
            .entrySet()
            .stream()
            .map(entry -> new FileTask(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

        // 2. Создание компаратора для сортировки
        Comparator<FileTask> comparator;
        if ("status".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(FileTask::getStatus);
        } else {
            comparator = Comparator.comparing(FileTask::getProcessingId);
        }

        // 3. Применение направления сортировки
        if (sortOrder.isDescending()) {
            comparator = comparator.reversed();
        }

        // 4. Сортировка списка
        tasks.sort(comparator);

        // 5. Пагинация
        int totalItems = tasks.size();
        int start      = (int) PageRequest.of(page, limit).getOffset();
        int end        = Math.min(start + limit, totalItems);
        
        if (start > totalItems) {
            return ResponseEntity.ok(Page.empty());
        }

        // 6. Создание объекта Page
        Page<FileTask> taskPage = new PageImpl<>(
            tasks.subList(start, end),
            PageRequest.of(page, limit, Sort.by(sortOrder, sortBy)),
            totalItems
        );

        return ResponseEntity.ok(taskPage);
    }
}

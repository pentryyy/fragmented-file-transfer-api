package com.pentryyy.fragmented_file_transfer_api.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.pentryyy.fragmented_file_transfer_api.exception.FileProcessingInterruptException;
import com.pentryyy.fragmented_file_transfer_api.model.FileTask;
import com.pentryyy.fragmented_file_transfer_api.service.FileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
@Tag(name = "Управление файлами", description = "Операции для загрузки, обработки и скачивания файлов")
public class FileController {

    @Autowired
    private FileService fileService;

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
        @RequestParam(value = "lossProbability", defaultValue = "0.3") double lossProbability,

        @Parameter(
            description = "Размер каждого чанка",
            example = "1024"
        ) 
        @RequestParam(value = "chunkSize", defaultValue = "1024") int chunkSize
    ) {
        
        JSONObject jsonObject = new JSONObject();

        String processingId;
        try {
            processingId = fileService.initializingFileProcessing(
                file, 
                lossProbability,
                chunkSize
            );
        } catch (IOException e) {
            return ResponseEntity.internalServerError() 
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(jsonObject.toString());
        }

        fileService.setupConfigureProcessing(processingId);

        Thread senderThread = new Thread(() -> {
            fileService.splittingFileIntoChunks(processingId);
        });
        
        // Старт потоков
        senderThread.start();
        
        // Ожидание завершения
        try {
            senderThread.join();
        } catch (InterruptedException e) {
            throw new FileProcessingInterruptException();
        }

        jsonObject.put("processingId", processingId);
        jsonObject.put("status", fileService.getStatusById(processingId));

        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
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
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Ошибка при обработке файла"
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
        
        Thread receiverThread = new Thread(() -> {
            fileService.assembleFileFromChunks(processingId);
        });
        
        // Старт потоков
        receiverThread.start();
        
        // Ожидание завершения
        try {
            receiverThread.join();
        } catch (InterruptedException e) {
            throw new FileProcessingInterruptException();
        }

        File file;
        try {
            file = fileService.getFileById(processingId);
           
            Resource resource = new FileSystemResource(file);

            ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(file.getName(), StandardCharsets.UTF_8)
                .build();
            
            return ResponseEntity.ok()
                                 .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                                 .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                 .body(resource);
            
        } catch (IOException e) {
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

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", fileService.getStatusById(processingId));
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }

    @Operation(
        summary = "Получение всех задач обработки",
        description = "Возвращает пагинированный список задач с возможностью сортировки"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Список задач обработки",
            content = @Content(
                mediaType = "application/json")
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

        Page<FileTask> fileTask = fileService.getAllTasks(
            page, 
            limit, 
            sortBy, 
            sortOrder
        );
        return ResponseEntity.ok(fileTask);
    }
}

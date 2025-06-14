package com.pentryyy.fragmented_file_transfer_api.controller;

import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.pentryyy.fragmented_file_transfer_api.transfer.core.TransmissionChannel;
import com.pentryyy.fragmented_file_transfer_api.transfer.receiver.FileAssembler;
import com.pentryyy.fragmented_file_transfer_api.transfer.sender.FileSplitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String RESOURCES_DIR = "src/main/resources/";

    private String currentOutputFilePath;
    private String currentProcessingId;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadAndProcessFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "lossProbability", defaultValue = "0.3") double lossProbability
    ) {
        
        JSONObject jsonObject = new JSONObject();

        try {
            // Создаем уникальный ID для обработки
            String processingId = UUID.randomUUID().toString();
            this.currentProcessingId = processingId;
            
            // Создаем директории для обработки
            String inputDir = RESOURCES_DIR + "input/" + processingId + "/";
            String outputDir = RESOURCES_DIR + "output/" + processingId + "/";

            Files.createDirectories(Paths.get(inputDir));
            Files.createDirectories(Paths.get(outputDir));

            // Сохраняем загруженный файл
            String originalFileName = file.getOriginalFilename();
            Path inputFilePath = Paths.get(inputDir + originalFileName);
            file.transferTo(inputFilePath);
            
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
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            jsonObject.put("processingId", processingId);
            jsonObject.put("status", "PROCESSING");

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

    @GetMapping("/download/{processingId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String processingId) {
        
        try {
            if (!processingId.equals(currentProcessingId)) {
                return ResponseEntity.notFound().build();
            }
            
            File file = new File(currentOutputFilePath);
            if (!file.exists()) {
                return ResponseEntity.status(404).body(null);
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                                 .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                 .body(resource);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/status/{processingId}")
    public ResponseEntity<String> getStatus(@PathVariable String processingId) {
        if (!processingId.equals(currentProcessingId)) {
            return ResponseEntity.notFound().build();
        }

        // Здесь будет реальная проверка статуса, но потом
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "COMPLETED");
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(jsonObject.toString());
    }
}

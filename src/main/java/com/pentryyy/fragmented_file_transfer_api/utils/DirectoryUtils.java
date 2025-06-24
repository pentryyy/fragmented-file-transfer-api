package com.pentryyy.fragmented_file_transfer_api.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.web.multipart.MultipartFile;

import com.pentryyy.fragmented_file_transfer_api.exception.ProcessedFileIsCorruptedException;

public class DirectoryUtils {
    private static final String RESOURCES_DIR = "src/main/resources/";

    public static String getOutputDir(String processingId) {
        return RESOURCES_DIR + "output/" + processingId + "/";
    }

    public static File convert(MultipartFile multipartFile) throws IOException {
        Path tempFile = Files.createTempFile("temp-", multipartFile.getOriginalFilename());
        Files.copy(multipartFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        
        return tempFile.toFile();
    }

    public static File getOutputFile(String processingId) {
        File outputDir = new File(getOutputDir(processingId));
        
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            throw new ProcessedFileIsCorruptedException("Каталог не найден: " + outputDir.getAbsolutePath());
        }
        
        File[] files = outputDir.listFiles(File::isFile);
        
        if (files == null || files.length == 0) {
            throw new ProcessedFileIsCorruptedException("В каталоге не найдено ни одного файла: " + outputDir.getAbsolutePath());
        }
        
        return files[0];
    }
}

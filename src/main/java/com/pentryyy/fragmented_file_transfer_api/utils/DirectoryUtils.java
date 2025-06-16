package com.pentryyy.fragmented_file_transfer_api.utils;

public class DirectoryUtils {
    private static final String RESOURCES_DIR = "src/main/resources/";

    public String getInputDir(String processingId) {
        return RESOURCES_DIR + "input/" + processingId + "/";
    }

    public String getOutputDir(String processingId) {
        return RESOURCES_DIR + "output/" + processingId + "/";
    }
}

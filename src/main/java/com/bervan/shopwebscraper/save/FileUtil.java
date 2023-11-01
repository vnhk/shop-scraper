package com.bervan.shopwebscraper.save;

import java.time.LocalDate;

public class FileUtil {
    public static String getFileName(String filenamePrefix, String extension) {
        LocalDate date = LocalDate.now();
        return filenamePrefix + date + extension;
    }
}

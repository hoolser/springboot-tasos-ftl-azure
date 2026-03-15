package com.tasos.demo.service;

import org.springframework.web.multipart.MultipartFile;

public interface SrtTranslationService {

    /**
     * Translate an SRT file from English to Greek
     * @param file the SRT file to translate
     * @return the translated SRT content as a string
     */
    String translateSrtFileEnToEl(MultipartFile file) throws Exception;

    /**
     * Check if file size is within limits
     * @param fileSize size in bytes
     * @return true if size is acceptable
     */
    boolean isFileSizeValid(long fileSize);

    /**
     * Get the maximum file size in bytes
     */
    long getMaxFileSizeBytes();

}


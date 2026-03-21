package com.tasos.demo.controller;

import com.tasos.demo.config.StorageConstants;
import com.tasos.demo.service.SrtTranslationService;
import com.tasos.demo.service.StorageBlobsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/srt/translation")
public class SrtTranslationController {

    private static final Logger logger = LoggerFactory.getLogger(SrtTranslationController.class);


    private final SrtTranslationService srtTranslationService;
    private final StorageBlobsService storageBlobsService;

    public SrtTranslationController(SrtTranslationService srtTranslationService,
                                    StorageBlobsService storageBlobsService) {
        this.srtTranslationService = srtTranslationService;
        this.storageBlobsService = storageBlobsService;
    }

    /**
     * Translate SRT file from English to Greek and save to shared container
     */
    @PostMapping("/translateEnToEl")
    public ResponseEntity<String> translateSrtEnToEl(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "provider", required = false) String provider) {
        logger.info("Received SRT file for translation: {}", file.getOriginalFilename());

        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file selected.");
            }

            // Check file size limit
            if (!srtTranslationService.isFileSizeValid(file.getSize())) {
                long maxMb = srtTranslationService.getMaxFileSizeBytes() / (1024 * 1024);
                return ResponseEntity.badRequest()
                        .body("File size exceeds maximum allowed size of " + maxMb + " MB");
            }

            // Check file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".srt")) {
                return ResponseEntity.badRequest()
                        .body("Invalid file format. Please upload a .srt file.");
            }

            // Translate the SRT file
            logger.info("Starting translation process for file: {} using provider: {}", originalFilename, provider);
            String translatedSrtContent = srtTranslationService.translateSrtFileEnToEl(file, provider);

            // Generate output filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(StorageConstants.FILE_TIMESTAMP_FORMAT));
            String originalNameWithoutExtension = originalFilename.replaceAll("\\.srt$", "");
            String outputFileName = StorageConstants.EN_EL_TRANSLATION_PREFIX + originalNameWithoutExtension + "_" + timestamp + StorageConstants.SRT_FILE_EXTENSION;

            logger.info("Saving translated file to shared container: {}", outputFileName);

            // Save translated SRT to shared container
            saveTranslatedSrtToContainer(outputFileName, translatedSrtContent);

            return ResponseEntity.ok()
                    .header("X-Filename", outputFileName)
                    .body("Translation completed successfully. File saved as: " + outputFileName);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Translation failed", e);
            return ResponseEntity.status(500)
                    .body("Translation failed: " + e.getMessage());
        }
    }

    /**
     * Download the translated SRT file
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadTranslatedSrt(String fileName) {
        logger.info("Downloading translated SRT file: {}", fileName);

        try {
            byte[] data = storageBlobsService.downloadFileFromContainer(StorageConstants.SHARE_CONTAINER, fileName);
            if (data == null) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (Exception e) {
            logger.error("Failed to download file", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * List all translated files
     */
    @GetMapping("/listTranslations")
    public ResponseEntity<String> listTranslatedFiles() {
        logger.info("Listing translated SRT files");

        try {
            java.util.List<String> fileNames = storageBlobsService.listFilesInContainer(StorageConstants.SHARE_CONTAINER);
            if (fileNames == null || fileNames.isEmpty()) {
                return ResponseEntity.ok("No translated files found.");
            }

            // Filter only translated files
            java.util.List<String> translatedFiles = fileNames.stream()
                    .filter(name -> name.startsWith(StorageConstants.EN_EL_TRANSLATION_PREFIX))
                    .toList();

            if (translatedFiles.isEmpty()) {
                return ResponseEntity.ok("No translated SRT files found.");
            }

            StringBuilder response = new StringBuilder("Translated SRT files:\n");
            for (String fileName : translatedFiles) {
                response.append("  - ").append(fileName).append("\n");
            }

            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            logger.error("Failed to list translated files", e);
            return ResponseEntity.status(500)
                    .body("Failed to list files: " + e.getMessage());
        }
    }

    /**
     * Get max file size in MB
     */
    @GetMapping("/maxFileSize")
    public ResponseEntity<String> getMaxFileSize() {
        long maxBytes = srtTranslationService.getMaxFileSizeBytes();
        long maxMb = maxBytes / (1024 * 1024);
        return ResponseEntity.ok("Maximum file size: " + maxMb + " MB");
    }

    @GetMapping("/provider")
    public ResponseEntity<String> getTranslationProvider() {
        String provider = srtTranslationService.getActiveProvider();
        return ResponseEntity.ok("Active translation provider: " + provider.toUpperCase());
    }

    /**
     * Save translated SRT content to shared container
     */
    private void saveTranslatedSrtToContainer(String fileName, String content) throws IOException {
        logger.debug("Converting translated content to MultipartFile");

        // Create a temporary multipart file-like structure
        byte[] contentBytes = content.getBytes("UTF-8");

        // Create a simple wrapper to pass to storage service
        org.springframework.web.multipart.MultipartFile multipartFile = new org.springframework.web.multipart.MultipartFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "text/plain";
            }

            @Override
            public boolean isEmpty() {
                return contentBytes.length == 0;
            }

            @Override
            public long getSize() {
                return contentBytes.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return contentBytes;
            }

            @Override
            public java.io.InputStream getInputStream() throws IOException {
                return new java.io.ByteArrayInputStream(contentBytes);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                java.nio.file.Files.write(dest.toPath(), contentBytes);
            }

            @Override
            public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
                java.nio.file.Files.write(dest, contentBytes);
            }
        };

        String result = storageBlobsService.uploadFileToContainer(StorageConstants.SHARE_CONTAINER, multipartFile);
        logger.info("File upload result: {}", result);
    }
}

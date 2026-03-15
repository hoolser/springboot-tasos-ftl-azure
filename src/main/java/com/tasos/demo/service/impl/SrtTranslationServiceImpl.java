package com.tasos.demo.service.impl;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.tasos.demo.config.StorageConstants;
import com.tasos.demo.model.SrtSubtitle;
import com.tasos.demo.service.SrtTranslationService;
import com.tasos.demo.util.SrtParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SrtTranslationServiceImpl implements SrtTranslationService {

    private static final Logger logger = LoggerFactory.getLogger(SrtTranslationServiceImpl.class);


    @Value("${deepl.api.key:}")
    private String deeplApiKey;

    private Translator translator;

    /**
     * Initialize DeepL Translator when needed
     */
    private synchronized Translator getTranslator() throws Exception {
        if (translator == null) {
            if (deeplApiKey == null || deeplApiKey.isEmpty()) {
                throw new IllegalStateException("DeepL API key not configured. Please set deepl.api.key in application properties.");
            }
            this.translator = new Translator(deeplApiKey);
        }
        return translator;
    }

    @Override
    public String translateSrtFileEnToEl(MultipartFile file) throws Exception {
        logger.info("Starting SRT file translation from EN to EL for file: {}", file.getOriginalFilename());

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (!isFileSizeValid(file.getSize())) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size of %d MB", StorageConstants.MAX_SRT_FILE_SIZE_MB)
            );
        }

        // Read file content
        String srtContent = readFileContent(file);

        // Validate SRT format
        if (!SrtParser.isSrtFile(srtContent)) {
            throw new IllegalArgumentException("Invalid SRT file format");
        }

        // Parse SRT
        List<SrtSubtitle> subtitles = SrtParser.parse(srtContent);
        logger.info("Parsed {} subtitles from the SRT file", subtitles.size());

        // Translate subtitles
        for (SrtSubtitle subtitle : subtitles) {
            String originalText = subtitle.getText();

            // Remove HTML tags for translation
            String textToTranslate = SrtParser.removeHtmlTags(originalText);

            logger.debug("Translating subtitle {}: {}", subtitle.getIndex(), textToTranslate);

            try {
                // Translate using DeepL
                // Source: English (EN-US), Target: Greek (EL)
                TextResult result = getTranslator().translateText(
                        textToTranslate,
                        "EN",
                        "EL"
                );

                String translatedText = result.getText();
                logger.debug("Translation result: {}", translatedText);

                // Restore HTML tags in the translated text
                String translatedWithTags = restoreHtmlTags(originalText, translatedText);
                subtitle.setText(translatedWithTags);

            } catch (com.deepl.api.DeepLException e) {
                logger.error("Failed to translate subtitle {}: {}", subtitle.getIndex(), e.getMessage());
                throw new RuntimeException("Translation failed for subtitle " + subtitle.getIndex(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Translation interrupted for subtitle {}: {}", subtitle.getIndex(), e.getMessage());
                throw new RuntimeException("Translation interrupted for subtitle " + subtitle.getIndex(), e);
            }
        }

        // Generate SRT output
        String translatedSrt = generateSrtContent(subtitles);
        logger.info("SRT translation completed successfully");

        return translatedSrt;
    }

    @Override
    public boolean isFileSizeValid(long fileSize) {
        return fileSize > 0 && fileSize <= StorageConstants.MAX_SRT_FILE_SIZE_BYTES;
    }

    @Override
    public long getMaxFileSizeBytes() {
        return StorageConstants.MAX_SRT_FILE_SIZE_BYTES;
    }

    /**
     * Read multipart file content as string
     */
    private String readFileContent(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Restore HTML tags from original text to translated text
     * This preserves the structure and tags from the original
     */
    private String restoreHtmlTags(String originalText, String translatedText) {
        // Extract all HTML tags from original text
        List<String> tags = extractHtmlTags(originalText);

        if (tags.isEmpty()) {
            // No tags to restore, return translated text as is
            return translatedText;
        }

        // Find opening and closing tags
        String openingTag = "";
        String closingTag = "";

        for (String tag : tags) {
            if (tag.startsWith("</")) {
                closingTag = tag;
            } else {
                openingTag = tag;
            }
        }

        // Wrap translated text with tags
        if (!openingTag.isEmpty() && !closingTag.isEmpty()) {
            return openingTag + translatedText + closingTag;
        } else if (!openingTag.isEmpty()) {
            return openingTag + translatedText;
        } else if (!closingTag.isEmpty()) {
            return translatedText + closingTag;
        }

        return translatedText;
    }

    /**
     * Extract HTML tags from text
     */
    private List<String> extractHtmlTags(String text) {
        List<String> tags = java.util.regex.Pattern.compile("<[^>]*>")
                .matcher(text)
                .results()
                .map(m -> m.group())
                .collect(Collectors.toList());
        return tags;
    }

    /**
     * Generate SRT formatted content from list of subtitles
     */
    private String generateSrtContent(List<SrtSubtitle> subtitles) {
        StringBuilder srtContent = new StringBuilder();

        for (int i = 0; i < subtitles.size(); i++) {
            SrtSubtitle subtitle = subtitles.get(i);

            srtContent.append(subtitle.getIndex()).append("\n");
            srtContent.append(subtitle.getTimecode()).append("\n");
            srtContent.append(subtitle.getText()).append("\n");

            // Add blank line between subtitles (except after last one)
            if (i < subtitles.size() - 1) {
                srtContent.append("\n");
            }
        }

        return srtContent.toString();
    }

}


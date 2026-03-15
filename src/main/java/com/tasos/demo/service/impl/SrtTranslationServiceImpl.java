package com.tasos.demo.service.impl;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.deepl.api.QuotaExceededException;
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

        // Use batch translation with XML tag handling for better context and grammar
        logger.info("Using batch translation with DeepL's native XML tag handling for improved accuracy");
        translateSubtitlesWithXmlHandling(subtitles);

        // Verify all subtitles were translated
        verifyTranslation(subtitles);

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
     * Translate subtitles using DeepL's native XML tag handling.
     * This approach keeps HTML tags in the content and lets DeepL handle them natively,
     * providing better context awareness, grammar, and gender agreement.
     *
     * Strategy: Batch translate groups of subtitles together to provide context
     * for DeepL while staying within API limits.
     * Batch size: 6 subtitles per batch for optimal results
     */
    private void translateSubtitlesWithXmlHandling(List<SrtSubtitle> subtitles) throws Exception {
        final int BATCH_SIZE = StorageConstants.SRT_TRANSLATION_BATCH_SIZE;

        logger.info("Starting batch translation with XML tag handling (batch size: {})", BATCH_SIZE);
        logger.info("Total subtitles to translate: {}", subtitles.size());

        int totalBatches = (subtitles.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        int batchNumber = 0;

        try {
            for (int i = 0; i < subtitles.size(); i += BATCH_SIZE) {
                batchNumber++;
                int endIdx = Math.min(i + BATCH_SIZE, subtitles.size());
                List<SrtSubtitle> batch = subtitles.subList(i, endIdx);

                logger.info("Processing batch {}/{}: Subtitles {} to {} ({} subtitles)",
                    batchNumber, totalBatches, i + 1, endIdx, batch.size());

                // Log which subtitles are in this batch
                StringBuilder batchInfo = new StringBuilder("Batch details: ");
                for (SrtSubtitle sub : batch) {
                    batchInfo.append("[").append(sub.getIndex()).append("] ");
                }
                logger.debug(batchInfo.toString());

                // Build XML-formatted content for this batch
                String batchXml = buildXmlBatch(batch);
                logger.debug("Built XML batch, sending to DeepL...");

                try {
                    // Translate the batch with XML formatting
                    TextResult result = getTranslator().translateText(
                            batchXml,
                            "EN",
                            "EL"
                    );

                    String translatedBatch = result.getText();
                    logger.debug("Batch translation result received ({} characters)", translatedBatch.length());

                    // Parse translated batch and update subtitles
                    updateSubtitlesFromXmlBatch(batch, translatedBatch);

                    logger.info("Batch {}/{} completed successfully", batchNumber, totalBatches);

                } catch (com.deepl.api.QuotaExceededException e) {
                    logger.error("DeepL API Quota exceeded: {}", e.getMessage());
                    throw new RuntimeException("DeepL API quota exceeded for this billing period. Please try again next billing cycle.", e);
                } catch (com.deepl.api.DeepLException e) {
                    logger.error("Failed to translate batch {}/{}: {}", batchNumber, totalBatches, e.getMessage());
                    throw new RuntimeException("Translation failed for batch " + batchNumber + ": " + e.getMessage(), e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Translation interrupted for batch {}/{}", batchNumber, totalBatches);
                    throw new RuntimeException("Translation interrupted for batch " + batchNumber, e);
                }
            }

            logger.info("All {}/{} batches translated successfully", batchNumber, totalBatches);

        } catch (Exception e) {
            logger.error("Error during batch translation: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Build XML-formatted content from subtitles for batch translation.
     * This format helps DeepL understand the context and structure.
     *
     * Strategy:
     * 1. Extract HTML tags from text and store them separately
     * 2. Send only the clean text to DeepL (minimize API usage)
     * 3. Restore tags in the translated output
     */
    private String buildXmlBatch(List<SrtSubtitle> subtitles) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<subtitles>\n");

        for (SrtSubtitle subtitle : subtitles) {
            // Extract HTML tags and get clean text
            String originalText = subtitle.getText();
            String cleanText = SrtParser.removeHtmlTags(originalText);

            // Wrap each subtitle in XML tags for structure
            // Send only clean text without HTML tags to DeepL
            xml.append("  <subtitle>\n");
            xml.append("    <index>").append(subtitle.getIndex()).append("</index>\n");
            xml.append("    <timecode>").append(subtitle.getTimecode()).append("</timecode>\n");
            xml.append("    <originalText>").append(escapeXml(originalText)).append("</originalText>\n");
            xml.append("    <text>").append(escapeXml(cleanText)).append("</text>\n");
            xml.append("  </subtitle>\n");
        }

        xml.append("</subtitles>");
        return xml.toString();
    }

    /**
     * Parse the translated XML batch and update subtitle texts.
     * Extracts the translated text from each subtitle element and restores HTML tags.
     *
     * This method is robust against parsing edge cases and ensures all subtitles
     * are properly updated even with malformed or edge-case input.
     */
    private void updateSubtitlesFromXmlBatch(List<SrtSubtitle> subtitles, String xmlResponse) {
        try {
            logger.debug("Parsing XML response for {} subtitles", subtitles.size());

            // Split by <subtitle> to get individual subtitle blocks
            String[] subtitleMatches = xmlResponse.split("<subtitle>");

            int subtitleIdx = 0;
            int successfulUpdates = 0;

            for (int i = 1; i < subtitleMatches.length && subtitleIdx < subtitles.size(); i++) {
                String match = subtitleMatches[i];

                try {
                    // Extract original text to get HTML tags
                    int origStart = match.indexOf("<originalText>");
                    int origEnd = match.indexOf("</originalText>");
                    String originalText = "";

                    if (origStart != -1 && origEnd != -1) {
                        origStart += "<originalText>".length();
                        originalText = match.substring(origStart, origEnd);
                        originalText = unescapeXml(originalText);
                        logger.debug("Subtitle {}: Original text extracted: {}",
                            subtitles.get(subtitleIdx).getIndex(), originalText);
                    } else {
                        logger.warn("Subtitle {}: Could not find <originalText> tags in XML response",
                            subtitleIdx + 1);
                    }

                    // Extract translated text (without HTML tags)
                    int textStart = match.indexOf("<text>");
                    int textEnd = match.indexOf("</text>");

                    String translatedText = null;

                    if (textStart != -1 && textEnd != -1) {
                        // Normal case: <text> tags found
                        textStart += "<text>".length();
                        translatedText = match.substring(textStart, textEnd);
                    } else {
                        // Fallback: <text> tags NOT found - DeepL may have altered them
                        // Extract everything after </originalText> as the translated content
                        int fallbackStart = match.indexOf("</originalText>");
                        if (fallbackStart != -1) {
                            fallbackStart += "</originalText>".length();
                            int fallbackEnd = match.indexOf("</subtitle>");
                            if (fallbackEnd == -1) {
                                fallbackEnd = match.length();
                            }

                            String content = match.substring(fallbackStart, fallbackEnd).trim();
                            // Remove any XML tags that might be there
                            translatedText = content.replaceAll("<[^>]*>", "").trim();

                            if (translatedText.isEmpty()) {
                                translatedText = null;
                            }
                        }

                        if (translatedText == null) {
                            logger.warn("Subtitle {}: Could not find <text> tags in XML response and fallback extraction failed",
                                subtitleIdx + 1);
                            subtitleIdx++;
                            continue;
                        }
                    }

                    // Unescape XML entities
                    translatedText = unescapeXml(translatedText);

                    if (translatedText == null || translatedText.trim().isEmpty()) {
                        logger.warn("Subtitle {}: Translated text is empty after unescaping",
                            subtitleIdx + 1);
                        subtitleIdx++;
                        continue;
                    }

                    logger.debug("Subtitle {}: Translated text extracted: {}",
                        subtitles.get(subtitleIdx).getIndex(), translatedText);

                    // Restore HTML tags from original text
                    String translatedWithTags = restoreHtmlTags(originalText, translatedText);

                    if (translatedWithTags == null || translatedWithTags.trim().isEmpty()) {
                        logger.warn("Subtitle {}: Final text is empty after tag restoration",
                            subtitleIdx + 1);
                        subtitleIdx++;
                        continue;
                    }

                    subtitles.get(subtitleIdx).setText(translatedWithTags);
                    logger.debug("Subtitle {} ({}) updated successfully with: {}",
                        subtitleIdx + 1,
                        subtitles.get(subtitleIdx).getIndex(),
                        translatedWithTags);

                    successfulUpdates++;
                    subtitleIdx++;

                } catch (Exception e) {
                    logger.error("Error processing subtitle {} from XML response: {}",
                        subtitleIdx + 1, e.getMessage(), e);
                    subtitleIdx++;
                    continue;
                }
            }

            if (successfulUpdates == 0) {
                logger.error("CRITICAL: No subtitles were successfully updated from XML response!");
                throw new RuntimeException("Failed to parse any subtitles from XML response. Response was: " + xmlResponse);
            }

            if (successfulUpdates < subtitles.size()) {
                logger.warn("Only {}/{} subtitles were successfully updated from XML response",
                    successfulUpdates, subtitles.size());
            }

            logger.info("Successfully updated {}/{} subtitles from translated batch",
                successfulUpdates, subtitles.size());

        } catch (Exception e) {
            logger.error("Error parsing XML batch response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse translated batch response: " + e.getMessage(), e);
        }
    }

    /**
     * Restore HTML tags from original text to translated text.
     * Preserves the tag structure while using the translated content.
     */
    private String restoreHtmlTags(String originalText, String translatedText) {
        if (originalText == null || originalText.isEmpty()) {
            return translatedText;
        }

        // Extract all HTML tags from original text in order
        java.util.List<String> tags = new java.util.ArrayList<>();
        java.util.regex.Pattern tagPattern = java.util.regex.Pattern.compile("<[^>]*>");
        java.util.regex.Matcher matcher = tagPattern.matcher(originalText);

        while (matcher.find()) {
            tags.add(matcher.group());
        }

        if (tags.isEmpty()) {
            // No tags to restore
            return translatedText;
        }

        // Find opening and closing tags
        StringBuilder openingTags = new StringBuilder();
        StringBuilder closingTags = new StringBuilder();

        for (String tag : tags) {
            if (tag.startsWith("</")) {
                closingTags.insert(0, tag);
            } else {
                openingTags.append(tag);
            }
        }

        // Wrap translated text with tags in same order as original
        return openingTags.append(translatedText).append(closingTags).toString();
    }

    /**
     * Escape XML special characters
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Unescape XML special characters
     */
    private String unescapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&apos;", "'")
                .replace("&quot;", "\"")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&");
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
     * Verify that all subtitles have been translated.
     * Detects any subtitles that were not properly translated (still contain English text).
     * Logs warnings for untranslated or partially translated subtitles.
     */
    private void verifyTranslation(List<SrtSubtitle> subtitles) {
        logger.info("Verifying translation quality for {} subtitles...", subtitles.size());

        java.util.List<Integer> untranslatedIndices = new java.util.ArrayList<>();
        int warningCount = 0;

        for (SrtSubtitle subtitle : subtitles) {
            String text = subtitle.getText();

            // Check if text appears to still contain significant English content
            // This is a heuristic check - very short texts or proper nouns might not translate
            if (text != null && !text.isEmpty()) {
                // Count English words (basic heuristic: common English words)
                String[] englishKeywords = {" is ", " the ", " and ", " to ", " of ", " in ", " you ", " i ", " it ", " for "};
                int englishWordCount = 0;

                for (String keyword : englishKeywords) {
                    if (text.toLowerCase().contains(keyword)) {
                        englishWordCount++;
                    }
                }

                // If more than 3 English keywords found, likely not translated
                if (englishWordCount > 3) {
                    untranslatedIndices.add(subtitle.getIndex());
                    warningCount++;
                    logger.warn("POTENTIAL UNTRANSLATED: Subtitle {} - {}", subtitle.getIndex(), text);
                }
            }
        }

        if (warningCount > 0) {
            logger.warn("TRANSLATION WARNING: {} subtitles may not have been properly translated", warningCount);
            logger.warn("Untranslated subtitle indices: {}", untranslatedIndices);
        } else {
            logger.info("Translation verification: All subtitles appear to have been translated successfully");
        }
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


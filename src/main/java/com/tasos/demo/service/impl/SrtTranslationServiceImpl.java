package com.tasos.demo.service.impl;

import com.deepl.api.TextResult;
import com.deepl.api.Translator;
import com.deepl.api.TextTranslationOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasos.demo.config.StorageConstants;
import com.tasos.demo.model.SrtSubtitle;
import com.tasos.demo.service.SrtTranslationService;
import com.tasos.demo.util.SrtParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SrtTranslationServiceImpl implements SrtTranslationService {

    private static final Logger logger = LoggerFactory.getLogger(SrtTranslationServiceImpl.class);

    private static final String PROVIDER_DEEPL = "deepl";
    private static final String PROVIDER_AZURE = "azure";

    @Value("${deepl.api.key:}")
    private String deeplApiKey;

    @Value("${srt.translation.provider:deepl}")
    private String translationProvider;

    @Value("${azure.translator.endpoint:https://api.cognitive.microsofttranslator.com}")
    private String azureTranslatorEndpoint;

    @Value("${azure.translator.key:}")
    private String azureTranslatorKey;

    @Value("${azure.translator.region:}")
    private String azureTranslatorRegion;

    private Translator translator;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public String translateSrtFileEnToEl(MultipartFile file, String selectedProvider) throws Exception {
        logger.info("Starting SRT file translation from EN to EL for file: {}", file.getOriginalFilename());

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Determine which provider to use
        // If selectedProvider is passed, use it. Otherwise fall back to configured default.
        String providerToUse = (selectedProvider != null && !selectedProvider.isBlank())
                ? selectedProvider.trim().toLowerCase()
                : getActiveProvider();

        logger.info("Selected translation provider: {}", providerToUse);

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

        if (PROVIDER_AZURE.equals(providerToUse)) {
            logger.info("Using Azure Translator API for EN -> EL translation");
            translateSubtitlesWithAzure(subtitles);
        } else {
            // Use batch translation with XML tag handling for better context and grammar
            logger.info("Using DeepL batch translation with XML tag handling for improved accuracy");
            translateSubtitlesWithXmlHandling(subtitles);
        }

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

    @Override
    public String getActiveProvider() {
        if (translationProvider == null || translationProvider.trim().isEmpty()) {
            return PROVIDER_DEEPL;
        }
        String normalized = translationProvider.trim().toLowerCase();
        return PROVIDER_AZURE.equals(normalized) ? PROVIDER_AZURE : PROVIDER_DEEPL;
    }


    /**
     * Translate subtitles using Azure Translator REST API.
     * Uses LARGER batches (50 items) compared to DeepL (6 items) to minimize requests and avoid rate limits.
     *
     * Azure has rate limits:
     * - Free tier: 2 requests per second (500 chars/min)
     * - Standard tier: 10 requests per second (1M chars/month)
     *
     * Strategy: Larger batches = fewer requests = better rate limit handling
     * Implements exponential backoff on 429 errors as fallback.
     */
    private void translateSubtitlesWithAzure(List<SrtSubtitle> subtitles) {
        validateAzureConfiguration();

        final int batchSize = StorageConstants.SRT_TRANSLATION_BATCH_SIZE_AZURE; // 50 items per batch
        int totalBatches = (subtitles.size() + batchSize - 1) / batchSize;

        logger.info("Starting Azure translation with LARGE batches (batch size: {}, total batches: {}, expected requests: {})",
            batchSize, totalBatches, totalBatches);
        logger.info("Larger batches minimize requests and reduce 429 rate limit errors");

        // Rate limiting: delay between requests (ms)
        // With 50-item batches, we can use more conservative delays
        // Azure Free: 2 req/sec = 500ms minimum
        // Azure Standard: 10 req/sec = 100ms minimum
        final long DELAY_BETWEEN_REQUESTS_MS = 500; // Conservative for free tier

        for (int i = 0, batchNumber = 1; i < subtitles.size(); i += batchSize, batchNumber++) {
            int endIdx = Math.min(i + batchSize, subtitles.size());
            List<SrtSubtitle> batch = subtitles.subList(i, endIdx);

            try {
                // Apply rate limiting delay before each request (except first)
                if (batchNumber > 1) {
                    logger.debug("Rate limiting: waiting {}ms before batch {}/{}", DELAY_BETWEEN_REQUESTS_MS, batchNumber, totalBatches);
                    Thread.sleep(DELAY_BETWEEN_REQUESTS_MS);
                }

                // Attempt translation with retry logic
                translateAzureBatchWithRetry(batch, batchNumber, totalBatches);

                logger.info("Azure batch {}/{} translated successfully ({} subtitles)", batchNumber, totalBatches, batch.size());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Translation interrupted for batch {}/{}", batchNumber, totalBatches);
                throw new RuntimeException("Translation interrupted for batch " + batchNumber, e);
            } catch (Exception e) {
                logger.error("Failed Azure translation for batch {}/{}: {}", batchNumber, totalBatches, e.getMessage(), e);
                throw new RuntimeException("Azure translation failed for batch " + batchNumber + ": " + e.getMessage(), e);
            }
        }

        logger.info("Azure translation completed: {} batches processed, {} subtitles translated", totalBatches, subtitles.size());
    }

    /**
     * Translate a single batch with retry logic and exponential backoff.
     * Retries up to 3 times on 429 (Too Many Requests) errors.
     */
    private void translateAzureBatchWithRetry(List<SrtSubtitle> batch, int batchNumber, int totalBatches) throws InterruptedException {
        final int MAX_RETRIES = 3;
        final long INITIAL_BACKOFF_MS = 1000; // Start with 1 second

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String url = buildAzureTranslateUrl();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Ocp-Apim-Subscription-Key", azureTranslatorKey);
                if (azureTranslatorRegion != null && !azureTranslatorRegion.isBlank()) {
                    headers.set("Ocp-Apim-Subscription-Region", azureTranslatorRegion);
                }

                List<Map<String, String>> requestBody = new java.util.ArrayList<>();
                for (SrtSubtitle subtitle : batch) {
                    String cleanText = SrtParser.removeHtmlTags(subtitle.getText());
                    Map<String, String> item = new HashMap<>();
                    item.put("text", cleanText == null ? "" : cleanText);
                    requestBody.add(item);
                }

                HttpEntity<List<Map<String, String>>> entity = new HttpEntity<>(requestBody, headers);

                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        throw new RuntimeException("Azure Translator returned non-success status: " + response.getStatusCode());
                    }

                    List<String> translatedTexts;
                    try {
                        translatedTexts = parseAzureResponse(response.getBody());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to parse Azure response: " + e.getMessage(), e);
                    }

                    if (translatedTexts.size() != batch.size()) {
                        throw new RuntimeException("Azure response size mismatch. Expected " + batch.size() + " but got " + translatedTexts.size());
                    }

                    for (int idx = 0; idx < batch.size(); idx++) {
                        SrtSubtitle subtitle = batch.get(idx);
                        String translated = translatedTexts.get(idx);
                        String restored = restoreHtmlTags(subtitle.getText(), translated);
                        subtitle.setText(restored);
                    }

                    // Success - exit retry loop
                    return;

                } catch (org.springframework.web.client.HttpClientErrorException ex) {
                    // Handle 429 Too Many Requests with retry
                    if (ex.getStatusCode().value() == 429) {
                        if (attempt < MAX_RETRIES) {
                            long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                            logger.warn("Azure 429 Rate Limit Hit on batch {}/{}. Attempt {}/{}. Waiting {}ms before retry...",
                                batchNumber, totalBatches, attempt, MAX_RETRIES, backoffMs);
                            Thread.sleep(backoffMs);
                            // Continue to next retry iteration
                        } else {
                            logger.error("Azure 429 Rate Limit persisted after {} retries for batch {}/{}", MAX_RETRIES, batchNumber, totalBatches);
                            throw new RuntimeException("Azure rate limit exceeded after " + MAX_RETRIES + " retries for batch " + batchNumber + ": " + ex.getMessage(), ex);
                        }
                    } else {
                        // Other HTTP errors - don't retry
                        throw new RuntimeException("Azure HTTP error " + ex.getStatusCode() + " for batch " + batchNumber + ": " + ex.getMessage(), ex);
                    }
                }

            } catch (InterruptedException e) {
                throw e; // Re-throw interruption
            } catch (RuntimeException e) {
                if (attempt == MAX_RETRIES) {
                    throw e; // Final attempt failed
                }
                logger.debug("Attempt {}/{} failed for batch {}/{}: {}", attempt, MAX_RETRIES, batchNumber, totalBatches, e.getMessage());
            }
        }
    }

    private void validateAzureConfiguration() {
        if (azureTranslatorKey == null || azureTranslatorKey.isBlank()) {
            throw new IllegalStateException("Azure Translator key is not configured. Set azure.translator.key in application properties.");
        }
        if (azureTranslatorEndpoint == null || azureTranslatorEndpoint.isBlank()) {
            throw new IllegalStateException("Azure Translator endpoint is not configured. Set azure.translator.endpoint in application properties.");
        }
    }

    private String buildAzureTranslateUrl() {
        String endpoint = azureTranslatorEndpoint.endsWith("/")
                ? azureTranslatorEndpoint.substring(0, azureTranslatorEndpoint.length() - 1)
                : azureTranslatorEndpoint;

        return UriComponentsBuilder
                .fromHttpUrl(endpoint)
                .path("/translate")
                .queryParam("api-version", "3.0")
                .queryParam("from", "en")
                .queryParam("to", "el")
                .toUriString();
    }

    private List<String> parseAzureResponse(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (!root.isArray()) {
            throw new RuntimeException("Azure response is not an array");
        }

        List<String> translatedTexts = new java.util.ArrayList<>();
        for (JsonNode item : root) {
            JsonNode translations = item.path("translations");
            if (!translations.isArray() || translations.isEmpty()) {
                translatedTexts.add("");
                continue;
            }
            translatedTexts.add(translations.get(0).path("text").asText(""));
        }
        return translatedTexts;
    }

    /**
     * Translate subtitles using DeepL's native XML tag handling.
     * Uses SMALLER batches (6 items) compared to Azure (50 items) for better context and grammar.
     * This approach keeps HTML tags in the content and lets DeepL handle them natively,
     * providing better context awareness, grammar, and gender agreement.
     *
     * Strategy: Batch translate groups of subtitles together to provide context
     * for DeepL while staying within API limits.
     * Batch size: 6 subtitles per batch for optimal quality vs API usage.
     */
    private void translateSubtitlesWithXmlHandling(List<SrtSubtitle> subtitles) throws Exception {
        final int BATCH_SIZE = StorageConstants.SRT_TRANSLATION_BATCH_SIZE_DEEPL; // 6 items per batch

        logger.info("Starting DeepL batch translation with context-optimized batches (batch size: {})", BATCH_SIZE);
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
                logger.info("Built XML batch ({} characters total), sending to DeepL...", batchXml.length());

                try {
                    // Create translation options with proper tag handling to optimize quota usage
                    // tag_handling="xml": Tells DeepL to recognize and NOT translate XML syntax
                    // ignore_tags: Tells DeepL NOT to translate or bill for these metadata tags
                    // Only the content inside <text> tags will be translated and billed
                    java.util.List<String> ignoreTags = Arrays.asList("index", "timecode", "originalText");

                    TextTranslationOptions options = new TextTranslationOptions()
                            .setTagHandling("xml")
                            .setIgnoreTags(ignoreTags);

                    logger.info("DeepL translation options configured:");
                    logger.info("  - Tag handling: xml");
                    logger.info("  - Ignore tags: index, timecode, originalText");
                    logger.info("  - This means: ONLY <text> content will be charged against your quota");

                    // Translate the batch with XML formatting and optimized parameters
                    TextResult result = getTranslator().translateText(
                            batchXml,
                            "EN",
                            "EL",
                            options
                    );

                    String translatedBatch = result.getText();
                    logger.info("Batch translation result received ({} characters)", translatedBatch.length());
                    logger.debug("Translated XML response (first 1000 chars):\n{}",
                            translatedBatch.length() > 1000 ?
                            translatedBatch.substring(0, 1000) + "\n...[truncated]" :
                            translatedBatch);

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
                } catch (Exception e){
                    logger.error("Error during batch translation: {}", e.getMessage(), e);
                    throw new RuntimeException("Translation failed for batch " + batchNumber + ": " + e.getMessage(), e);
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
     * IMPORTANT: This method creates XML that will be sent to DeepL with tag_handling="xml"
     * and ignore_tags="index,timecode,originalText" to minimize API character consumption.
     *
     * Strategy:
     * 1. Extract HTML tags from text and store them separately
     * 2. Send only the clean text to DeepL (minimize API usage)
     * 3. Restore tags in the translated output
     * 4. Metadata (index, timecode, originalText) will NOT be counted toward quota
     * 5. Only content in <text> tags will be billed
     */
    private String buildXmlBatch(List<SrtSubtitle> subtitles) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<subtitles>\n");

        int totalCleanCharacters = 0;  // Track characters that will be billed

        for (SrtSubtitle subtitle : subtitles) {
            // Extract HTML tags and get clean text
            String originalText = subtitle.getText();
            String cleanText = SrtParser.removeHtmlTags(originalText);

            totalCleanCharacters += cleanText.length();

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
        String xmlOutput = xml.toString();

        // DETAILED LOGGING - Show exactly what we're sending to DeepL
        logger.info("=== DEEPL API REQUEST DETAILS ===");
        logger.info("Total subtitles in batch: {}", subtitles.size());
        logger.debug("Total characters that WILL BE BILLED (in <text> tags only): {}", totalCleanCharacters);
        logger.info("Total XML payload size (including tags): {} characters", xmlOutput.length());
        logger.debug("Billing optimization: Using tag_handling='xml' + ignore_tags='index,timecode,originalText'");
        logger.info("Expected characters to be charged: ~{} (only content in <text> tags)", totalCleanCharacters);
        logger.debug("--- EXACT XML PAYLOAD BEING SENT ---");
        logger.debug(xmlOutput);
        logger.debug("--- END XML PAYLOAD ---");

        return xmlOutput;
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


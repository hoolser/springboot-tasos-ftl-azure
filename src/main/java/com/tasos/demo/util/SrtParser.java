package com.tasos.demo.util;

import com.tasos.demo.model.SrtSubtitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SrtParser {
    private static final Logger logger = LoggerFactory.getLogger(SrtParser.class);

    // Pattern to match SRT timecode: "00:01:03,812 --> 00:01:07,248"
    private static final Pattern TIMECODE_PATTERN = Pattern.compile("\\d{2}:\\d{2}:\\d{2},\\d{3}\\s*-->\\s*\\d{2}:\\d{2}:\\d{2},\\d{3}");

    // Pattern to match index lines (just numbers)
    private static final Pattern INDEX_PATTERN = Pattern.compile("^\\d+$");

    public static List<SrtSubtitle> parse(String srtContent) throws IllegalArgumentException {
        List<SrtSubtitle> subtitles = new ArrayList<>();

        if (srtContent == null || srtContent.trim().isEmpty()) {
            throw new IllegalArgumentException("SRT content cannot be empty");
        }

        String[] lines = srtContent.split("\n");
        int i = 0;

        while (i < lines.length) {
            String line = lines[i].trim();

            // Skip empty lines
            if (line.isEmpty()) {
                i++;
                continue;
            }

            // Check if this is an index line
            if (INDEX_PATTERN.matcher(line).matches()) {
                try {
                    int index = Integer.parseInt(line);
                    i++;

                    // Next line should be timecode
                    if (i >= lines.length) {
                        break;
                    }

                    String timecodeLine = lines[i].trim();
                    if (!TIMECODE_PATTERN.matcher(timecodeLine).find()) {
                        logger.warn("Invalid timecode format: {}", timecodeLine);
                        i++;
                        continue;
                    }

                    i++;

                    // Collect text lines until we hit an empty line or another index
                    StringBuilder textBuilder = new StringBuilder();
                    while (i < lines.length) {
                        String textLine = lines[i].trim();

                        if (textLine.isEmpty()) {
                            i++;
                            break;
                        }

                        // Check if this is the start of a new subtitle
                        if (INDEX_PATTERN.matcher(textLine).matches() ||
                            TIMECODE_PATTERN.matcher(textLine).find()) {
                            break;
                        }

                        if (textBuilder.length() > 0) {
                            textBuilder.append(" ");
                        }
                        textBuilder.append(textLine);
                        i++;
                    }

                    String text = textBuilder.toString().trim();
                    if (!text.isEmpty()) {
                        SrtSubtitle subtitle = new SrtSubtitle(index, timecodeLine, text);
                        subtitles.add(subtitle);
                    }

                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse subtitle index", e);
                    i++;
                }
            } else {
                i++;
            }
        }

        if (subtitles.isEmpty()) {
            throw new IllegalArgumentException("No valid subtitles found in SRT content");
        }

        return subtitles;
    }

    /**
     * Remove HTML/XML tags from text but preserve the structure
     */
    public static String removeHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        // Remove HTML tags like <i>, </i>, <b>, </b>, etc.
        return text.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Check if the file is a valid SRT file by checking its format
     */
    public static boolean isSrtFile(String srtContent) {
        if (srtContent == null || srtContent.trim().isEmpty()) {
            return false;
        }

        try {
            List<SrtSubtitle> subtitles = parse(srtContent);
            return !subtitles.isEmpty();
        } catch (IllegalArgumentException e) {
            logger.debug("Invalid SRT format: {}", e.getMessage());
            return false;
        }
    }
}


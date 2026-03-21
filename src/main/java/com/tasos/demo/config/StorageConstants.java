package com.tasos.demo.config;

/**
 * Global constants for storage operations and file size limits.
 * Centralized configuration for all storage-related constraints, paths, and naming conventions.
 */
public final class StorageConstants {

    // ================== File Size Limits ==================

    /**
     * Maximum total storage size in MB for blob storage operations
     */
    public static final long MAX_TOTAL_SIZE_MB = 100;

    /**
     * Maximum total storage size in bytes for blob storage operations
     */
    public static final long MAX_TOTAL_SIZE_BYTES = MAX_TOTAL_SIZE_MB * 1024 * 1024;

    /**
     * Maximum SRT file size in MB for translation operations
     */
    public static final long MAX_SRT_FILE_SIZE_MB = 50;

    /**
     * Maximum SRT file size in bytes for translation operations
     */
    public static final long MAX_SRT_FILE_SIZE_BYTES = MAX_SRT_FILE_SIZE_MB * 1024 * 1024;

    // ================== Translation Configuration ==================

    /**
     * Batch size for SRT translation using DeepL API.
     * Groups this many subtitles together for batch translation to provide
     * context for improved gender agreement and grammar in Greek.
     *
     * Optimal value = 6:
     * - Captures typical scene dialogue (4-6 lines per scene)
     * - Provides good context for DeepL without diluting with unrelated dialogue
     * - Well under API limits
     * - No performance penalty vs smaller batches
     */
    public static final int SRT_TRANSLATION_BATCH_SIZE_DEEPL = 6;

    /**
     * Batch size for SRT translation using Azure Translator API.
     * Groups this many subtitles together to minimize API requests and avoid rate limits.
     *
     * Optimal value = 50:
     * - Azure supports up to 100 items per request
     * - Larger batches = fewer requests = avoid 429 rate limit errors
     * - Reduces character count per second (respects rate limits)
     * - Azure charges per character, not per request (cost-efficient)
     * - Still provides reasonable context for translation quality
     */
    public static final int SRT_TRANSLATION_BATCH_SIZE_AZURE = 50;

    /**
     * Container name for shared blob storage.
     * Used for storing translated SRT files and shared resources.
     */
    public static final String SHARE_CONTAINER = "tasos-shared-container";


    /**
     * Default local storage directory name (relative to user home).
     * Full path can be configured via 'local-storage-path' property in application.properties.
     * Default: ${user.home}/tasos-storage
     */
    public static final String DEFAULT_LOCAL_STORAGE_DIR = "tasos-storage";

    /**
     * Property key for configuring local storage path in application.properties
     */
    public static final String STORAGE_PATH_PROPERTY_KEY = "local-storage-path";

    // ================== File Naming Prefixes ==================

    /**
     * Prefix for translated SRT files (English to Greek).
     * Example: translated_en_el_movie_20260315_180700.srt
     */
    public static final String EN_EL_TRANSLATION_PREFIX = "translated_en_el_";

    /**
     * Date-time format pattern for file naming
     */
    public static final String FILE_TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";

    // ================== File Extensions ==================

    /**
     * SRT subtitle file extension
     */
    public static final String SRT_FILE_EXTENSION = ".srt";

    // Private constructor to prevent instantiation
    private StorageConstants() {
        throw new AssertionError("Cannot instantiate StorageConstants utility class");
    }
}


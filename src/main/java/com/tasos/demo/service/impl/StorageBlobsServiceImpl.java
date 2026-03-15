package com.tasos.demo.service.impl;

import com.tasos.demo.config.StorageConstants;
import com.tasos.demo.service.StorageBlobsService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageBlobsServiceImpl implements StorageBlobsService {

    private static final Logger logger = LoggerFactory.getLogger(StorageBlobsServiceImpl.class);

    // In-memory metadata store (directory name → metadata map)
    private final Map<String, Map<String, String>> containerMetadataStore = new HashMap<>();

    @Value("${" + StorageConstants.STORAGE_PATH_PROPERTY_KEY + ":${user.home}/" + StorageConstants.DEFAULT_LOCAL_STORAGE_DIR + "}")
    private String storagePath;

    private Path storageRoot;

    @PostConstruct
    public void initialize() {
        try {
            storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(storageRoot)) {
                Files.createDirectories(storageRoot);
                logger.info("Local storage directory created at: {}", storageRoot);
            } else {
                logger.info("Using local storage directory: {}", storageRoot);
            }
        } catch (IOException e) {
            logger.error("Failed to initialize local storage", e);
            throw new RuntimeException("Failed to initialize local storage", e);
        }
    }

    private Path getDirectoryPath(String directoryName) {
        return storageRoot.resolve(directoryName).normalize();
    }

    private void validateDirectoryPath(Path directoryPath) throws IOException {
        if (!directoryPath.toAbsolutePath().normalize().startsWith(storageRoot)) {
            throw new IOException("Invalid directory path: security check failed");
        }
    }

    @Override
    public boolean test() {
        return true;
    }

    @Override
    public List<String> listContainers() {
        List<String> directoryNames = new ArrayList<>();
        try {
            File[] directories = storageRoot.toFile().listFiles(File::isDirectory);
            if (directories != null) {
                for (File directory : directories) {
                    directoryNames.add(directory.getName());
                    logger.info("Found directory: {}", directory.getName());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to list directories", e);
        }
        return directoryNames;
    }

    @Override
    public String createUniqueContainer(String containerName) {
        try {
            if (containerName == null || containerName.isEmpty()) {
                return "Directory name cannot be empty";
            }

            Path directoryPath = getDirectoryPath(containerName);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                logger.info("Directory '{}' has been created at: {}", containerName, directoryPath);
                return containerName;
            } else {
                logger.info("Directory '{}' already exists", containerName);
                return containerName;
            }
        } catch (Exception e) {
            logger.error("Failed to create directory", e);
            return e.getMessage();
        }
    }

    @Override
    public String uploadTestFileToContainer(String containerName) {
        try {
            if (containerName == null || containerName.isEmpty()) {
                return "Directory name cannot be empty";
            }

            Path directoryPath = getDirectoryPath(containerName);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath)) {
                logger.error("Directory '{}' does not exist.", containerName);
                return "Directory does not exist";
            }

            String fileName = "test-file.txt";
            String fileContent = "This is a test file uploaded to local storage.";
            Path filePath = directoryPath.resolve(fileName);

            Files.write(filePath, fileContent.getBytes());
            logger.info("Test file '{}' uploaded to directory '{}'.", fileName, containerName);
            return "Test file uploaded successfully";
        } catch (Exception e) {
            logger.error("Failed to upload test file to directory", e);
            return e.getMessage();
        }
    }

    @Override
    public List<String> listFilesInContainer(String containerName) {
        List<String> fileNames = new ArrayList<>();
        try {
            Path directoryPath = getDirectoryPath(containerName);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", containerName);
                return null;
            }

            File[] files = directoryPath.toFile().listFiles(File::isFile);
            if (files != null) {
                for (File file : files) {
                    logger.info("Found file: {}", file.getName());
                    fileNames.add(file.getName());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to list files in directory", e);
        }
        return fileNames;
    }

    @Override
    public List<byte[]> downloadBlobsFromContainer(String containerName) {
        List<byte[]> filesData = new ArrayList<>();
        try {
            Path directoryPath = getDirectoryPath(containerName);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", containerName);
                return filesData;
            }

            File[] files = directoryPath.toFile().listFiles(File::isFile);
            if (files != null) {
                for (File file : files) {
                    try {
                        byte[] data = Files.readAllBytes(file.toPath());
                        filesData.add(data);
                        logger.info("Read file '{}'", file.getName());
                    } catch (Exception ex) {
                        logger.error("Failed to read file '{}'", file.getName(), ex);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to download files from directory", e);
        }
        return filesData;
    }

    @Override
    public String deleteContainer(String containerName) {
        try {
            Path directoryPath = getDirectoryPath(containerName);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", containerName);
                return "Container does not exist";
            }

            // Delete all files inside first, then the directory
            File[] files = directoryPath.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    Files.delete(file.toPath());
                }
            }
            Files.delete(directoryPath);
            containerMetadataStore.remove(containerName);
            logger.info("Directory '{}' deleted successfully.", containerName);
            return "Container deleted successfully";
        } catch (Exception e) {
            logger.error("Failed to delete directory", e);
            return e.getMessage();
        }
    }

    @Override
    public String uploadFileToContainer(String container, MultipartFile file) {
        try {
            Path directoryPath = getDirectoryPath(container);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", container);
                return "Container does not exist";
            }

            // Calculate current total size
            long currentTotalSize = 0;
            File[] existing = directoryPath.toFile().listFiles(File::isFile);
            if (existing != null) {
                for (File f : existing) {
                    currentTotalSize += f.length();
                }
            }

            long newFileSize = file.getSize();
            if (currentTotalSize + newFileSize > StorageConstants.MAX_TOTAL_SIZE_BYTES) {
                return "Upload failed: total container size limit (" + StorageConstants.MAX_TOTAL_SIZE_MB + " MB) exceeded.";
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                return "Invalid file name.";
            }

            Path filePath = directoryPath.resolve(fileName).normalize();
            validateDirectoryPath(filePath.getParent());

            Files.write(filePath, file.getBytes());
            logger.info("File '{}' uploaded to directory '{}'.", fileName, container);
            return "File uploaded successfully";
        } catch (Exception e) {
            logger.error("Failed to upload file to directory", e);
            return e.getMessage();
        }
    }

    @Override
    public byte[] downloadFileFromContainer(String container, String fileName) {
        try {
            Path directoryPath = getDirectoryPath(container);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", container);
                return null;
            }

            Path filePath = directoryPath.resolve(fileName).normalize();
            validateDirectoryPath(filePath.getParent());

            if (!Files.exists(filePath)) {
                logger.error("File '{}' does not exist in directory '{}'.", fileName, container);
                return null;
            }

            byte[] data = Files.readAllBytes(filePath);
            logger.info("File '{}' downloaded from directory '{}'.", fileName, container);
            return data;
        } catch (Exception e) {
            logger.error("Failed to download file from directory", e);
            return null;
        }
    }

    @Override
    public String clearContainer(String container) {
        try {
            Path directoryPath = getDirectoryPath(container);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", container);
                return "Container does not exist";
            }

            int deleted = 0;
            File[] files = directoryPath.toFile().listFiles(File::isFile);
            if (files != null) {
                for (File file : files) {
                    Files.delete(file.toPath());
                    deleted++;
                }
            }
            logger.info("Deleted {} file(s) from directory '{}'.", deleted, container);
            return deleted + " file(s) deleted from container.";
        } catch (Exception e) {
            logger.error("Failed to clear directory", e);
            return e.getMessage();
        }
    }

    @Override
    public String readContainerProperties(String containerName) {
        try {
            Path directoryPath = getDirectoryPath(containerName);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", containerName);
                return "Container does not exist";
            }

            BasicFileAttributes attrs = Files.readAttributes(directoryPath, BasicFileAttributes.class);

            long totalSize = 0;
            int fileCount = 0;
            File[] files = directoryPath.toFile().listFiles(File::isFile);
            if (files != null) {
                for (File f : files) {
                    totalSize += f.length();
                    fileCount++;
                }
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

            StringBuilder propertiesStr = new StringBuilder();
            propertiesStr.append("Container Properties:\n");
            propertiesStr.append("  Path: ").append(directoryPath).append("\n");
            propertiesStr.append("  Created: ").append(fmt.format(attrs.creationTime().toInstant())).append("\n");
            propertiesStr.append("  Last Modified: ").append(fmt.format(attrs.lastModifiedTime().toInstant())).append("\n");
            propertiesStr.append("  File Count: ").append(fileCount).append("\n");
            propertiesStr.append("  Total Size: ").append(totalSize).append(" bytes (")
                    .append(String.format("%.2f", totalSize / (1024.0 * 1024.0))).append(" MB)\n");
            propertiesStr.append("  Metadata:\n");

            Map<String, String> metadata = containerMetadataStore.getOrDefault(containerName, new HashMap<>());
            if (!metadata.isEmpty()) {
                metadata.forEach((key, value) ->
                        propertiesStr.append("    ").append(key).append(": ").append(value).append("\n"));
            } else {
                propertiesStr.append("    No metadata found. Use addContainerMetadata() to add metadata.\n");
            }

            logger.info("Retrieved properties for directory '{}'", containerName);
            return propertiesStr.toString();
        } catch (Exception e) {
            logger.error("Failed to retrieve directory properties", e);
            return "Error retrieving container properties: " + e.getMessage();
        }
    }

    @Override
    public String addContainerMetadata(String containerName) {
        try {
            Path directoryPath = getDirectoryPath(containerName);
            validateDirectoryPath(directoryPath);

            if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
                logger.error("Directory '{}' does not exist.", containerName);
                return "Container does not exist";
            }

            Map<String, String> metadata = new HashMap<>();
            metadata.put("docType", "textDocuments");
            metadata.put("category", "guidance");

            containerMetadataStore.put(containerName, metadata);

            logger.info("Metadata added to directory '{}'.", containerName);
            return "Metadata added successfully to container";
        } catch (Exception e) {
            logger.error("Failed to add metadata to directory", e);
            return e.getMessage();
        }
    }

}

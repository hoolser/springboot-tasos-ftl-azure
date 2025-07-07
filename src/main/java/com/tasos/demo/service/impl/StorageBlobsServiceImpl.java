package com.tasos.demo.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobContainerProperties;
import com.tasos.demo.service.StorageBlobsService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageBlobsServiceImpl implements StorageBlobsService {

    private static final Logger logger = LoggerFactory.getLogger(StorageBlobsServiceImpl.class);

    @Value("${azure-storage-connection-string}")
    private String storageConnectionString;

    private BlobServiceClient blobServiceClient;

    @PostConstruct
    public void initialize() {
        blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();
        logger.info("BlobServiceClient initialized");
    }

    @Override
    public boolean test() {
        return true;
    }

    @Override
    public List<String> listContainers() {
        List<String> containerNames = new ArrayList<>();

        try {

            for (BlobContainerItem containerItem : blobServiceClient.listBlobContainers()) {
                String name = containerItem.getName();
                logger.info("Found container: {}", name);
                containerNames.add(name);
            }

        } catch (Exception e) {
            logger.error("Failed to list containers", e);
        }

        return containerNames;
    }

    @Override
    public String createUniqueContainer(String containerName) {
        try {

            // Create the container
            BlobContainerClient containerClient = blobServiceClient
                    .createBlobContainer(containerName);

            logger.info("A container named '{}' has been created. Verify it in the Azure portal.", containerName);
            logger.info("Next a file will be created and uploaded to the container.");

            return containerName;

        } catch (Exception e) {
            logger.error("Failed to create container", e);
            return e.getMessage();
        }
    }

    @Override
    public String uploadTestFileToContainer(String containerName) {
        try {

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", containerName);
                return "Container does not exist";
            }

            String blobName = "test-file.txt";
            String fileContent = "This is a test file uploaded to Azure Blob Storage.";
            byte[] contentBytes = fileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            containerClient.getBlobClient(blobName)
                    .upload(new java.io.ByteArrayInputStream(contentBytes), contentBytes.length, true);

            logger.info("Test file '{}' uploaded to container '{}'.", blobName, containerName);
            return "Test file uploaded successfully";
        } catch (Exception e) {
            logger.error("Failed to upload test file to container", e);
            return e.getMessage();
        }
    }

    @Override
    public List<String> listFilesInContainer(String containerName) {
        List<String> blobNames = new ArrayList<>();
        try {

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", containerName);
                return null;
            }

            containerClient.listBlobs().forEach(blobItem -> {
                logger.info("Found blob: {}", blobItem.getName());
                blobNames.add(blobItem.getName());
            });

        } catch (Exception e) {
            logger.error("Failed to list blobs in container", e);
        }
        return blobNames;
    }

    @Override
    public List<byte[]> downloadBlobsFromContainer(String containerName) {
        List<byte[]> filesData = new ArrayList<>();
        try {

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", containerName);
                return filesData;
            }

            containerClient.listBlobs().forEach(blobItem -> {
                String blobName = blobItem.getName();
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    containerClient.getBlobClient(blobName).download(outputStream);
                    filesData.add(outputStream.toByteArray());
                    logger.info("Downloaded blob '{}'", blobName);
                } catch (Exception ex) {
                    logger.error("Failed to download blob '{}'", blobName, ex);
                }
            });

        } catch (Exception e) {
            logger.error("Failed to download blobs from container", e);
        }
        return filesData;
    }

    @Override
    public String deleteContainer(String containerName) {
        try {

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", containerName);
                return "Container does not exist";
            }

            containerClient.delete();
            logger.info("Container '{}' deleted successfully.", containerName);
            return "Container deleted successfully";
        } catch (Exception e) {
            logger.error("Failed to delete container", e);
            return e.getMessage();
        }
    }


    public String uploadFileToContainer(String container, MultipartFile file) {
        final long MAX_TOTAL_SIZE_MB = 100; // upload limitation in size.
        final long MAX_TOTAL_SIZE_BYTES = MAX_TOTAL_SIZE_MB * 1024 * 1024;

        try {

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", container);
                return "Container does not exist";
            }

            // Calculate current total size
            long currentTotalSize = 0;
            for (var blobItem : containerClient.listBlobs()) {
                currentTotalSize += blobItem.getProperties().getContentLength();
            }

            long newFileSize = file.getSize();
            if (currentTotalSize + newFileSize > MAX_TOTAL_SIZE_BYTES) {
                return "Upload failed: total container size limit (" + MAX_TOTAL_SIZE_MB + " MB) exceeded.";
            }

            String blobName = file.getOriginalFilename();
            if (blobName == null || blobName.isEmpty()) {
                return "Invalid file name.";
            }

            containerClient.getBlobClient(blobName)
                    .upload(file.getInputStream(), newFileSize, true);

            logger.info("File '{}' uploaded to container '{}'.", blobName, container);
            return "File uploaded successfully";
        } catch (Exception e) {
            logger.error("Failed to upload file to container", e);
            return e.getMessage();
        }
    }

    public byte[] downloadFileFromContainer(String container, String fileName) {
        try {

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", container);
                return null;
            }

            if (!containerClient.getBlobClient(fileName).exists()) {
                logger.error("Blob '{}' does not exist in container '{}'.", fileName, container);
                return null;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            containerClient.getBlobClient(fileName).download(outputStream);
            logger.info("File '{}' downloaded from container '{}'.", fileName, container);
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to download file from container", e);
            return null;
        }
    }

    public String clearContainer(String container) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(container);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", container);
                return "Container does not exist";
            }

            int deleted = 0;
            for (var blobItem : containerClient.listBlobs()) {
                containerClient.getBlobClient(blobItem.getName()).delete();
                deleted++;
            }
            logger.info("Deleted {} blobs from container '{}'.", deleted, container);
            return deleted + " file(s) deleted from container.";
        } catch (Exception e) {
            logger.error("Failed to clear container", e);
            return e.getMessage();
        }
    }


    @Override
    public String readContainerProperties(String containerName) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", containerName);
                return "Container does not exist";
            }

            BlobContainerProperties properties = containerClient.getProperties();

            // Create a formatted string with all relevant properties
            StringBuilder propertiesStr = new StringBuilder();
            propertiesStr.append("Container Properties:\n");
            propertiesStr.append("  ETag: ").append(properties.getETag()).append("\n");
            propertiesStr.append("  Last Modified: ").append(properties.getLastModified()).append("\n");
            propertiesStr.append("  Lease Status: ").append(properties.getLeaseStatus()).append("\n");
            propertiesStr.append("  Lease State: ").append(properties.getLeaseState()).append("\n");
            propertiesStr.append("  Lease Duration: ").append(properties.getLeaseDuration()).append("\n");
            propertiesStr.append("  Public Access: ").append(properties.getBlobPublicAccess()).append("\n");
            propertiesStr.append("  Has Immutability Policy: ").append(properties.hasImmutabilityPolicy()).append("\n");
            propertiesStr.append("  Has Legal Hold: ").append(properties.hasLegalHold()).append("\n");

            // Include metadata with clear formatting
            propertiesStr.append("  Metadata:\n");
            if (properties.getMetadata() != null && !properties.getMetadata().isEmpty()) {
                properties.getMetadata().forEach((key, value) ->
                        propertiesStr.append("    ").append(key).append(": ").append(value).append("\n"));

                // Check for specific metadata keys we're interested in
                /*String docType = properties.getMetadata().getOrDefault("docType", "Not set");
                String category = properties.getMetadata().getOrDefault("category", "Not set");

                propertiesStr.append("\n  Important Metadata Values:\n");
                propertiesStr.append("    Document Type: ").append(docType).append("\n");
                propertiesStr.append("    Category: ").append(category).append("\n");*/
            } else {
                propertiesStr.append("    No metadata found. Use addContainerMetadata() to add metadata.\n");
            }

            logger.info("Retrieved properties for container '{}'", containerName);
            return propertiesStr.toString();

        } catch (Exception e) {
            logger.error("Failed to retrieve container properties", e);
            return "Error retrieving container properties: " + e.getMessage();
        }
    }

    @Override
    public String addContainerMetadata(String containerName) {
        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                logger.error("Container '{}' does not exist.", containerName);
                return "Container does not exist";
            }

            // Create a map to hold the metadata
            Map<String, String> metadata = new HashMap<>();

            // Add metadata to the container
            metadata.put("docType", "textDocuments");
            metadata.put("category", "guidance");

            // Set the container's metadata
            containerClient.setMetadata(metadata);

            logger.info("Metadata added to container '{}'.", containerName);
            return "Metadata added successfully to container";
        } catch (Exception e) {
            logger.error("Failed to add metadata to container", e);
            return e.getMessage();
        }
    }


}

package com.tasos.demo.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.tasos.demo.service.StorageBlobsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class StorageBlobsServiceImpl implements StorageBlobsService {

    private static final Logger logger = LoggerFactory.getLogger(StorageBlobsServiceImpl.class);

    @Value("${azure-storage-connection-string-temporary}")
    private String storageConnectionString;

    @Override
    public boolean test() {
        return true;
    }

    @Override
    public List<String> listContainers() {
        List<String> containerNames = new ArrayList<>();

        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(storageConnectionString)
                    .buildClient();

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
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(storageConnectionString)
                    .buildClient();

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
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(storageConnectionString)
                    .buildClient();

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
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(storageConnectionString)
                    .buildClient();

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
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(storageConnectionString)
                    .buildClient();

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
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(storageConnectionString)
                    .buildClient();

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


}

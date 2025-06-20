package com.tasos.demo.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AzureBlobAppender extends AppenderBase<ILoggingEvent> {
    private String connectionString;
    private String containerName;
    private String blobNamePattern;
    private Layout<ILoggingEvent> layout;
    private BlobContainerClient containerClient;
    private ByteArrayOutputStream logBuffer = new ByteArrayOutputStream();

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setBlobNamePattern(String blobNamePattern) {
        this.blobNamePattern = blobNamePattern;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    @Override
    public void start() {
        if (connectionString == null || containerName == null || blobNamePattern == null || layout == null) {
            addError("Missing required properties for AzureBlobAppender");
            return;
        }

        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = blobServiceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
            }

            super.start();
        } catch (Exception e) {
            addError("Failed to initialize Azure Blob Storage", e);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        try {
            String formattedMessage = layout.doLayout(event);
            logBuffer.write(formattedMessage.getBytes(StandardCharsets.UTF_8));

            // Upload periodically or on certain events
            if (event.getLevel().toInt() >= ch.qos.logback.classic.Level.ERROR_INT ||
                    logBuffer.size() > 1024 * 1024) { // 1MB threshold
                uploadToBlob();
            }
        } catch (IOException e) {
            addError("Failed to append log message", e);
        }
    }

    @Override
    public void stop() {
        if (isStarted()) {
            try {
                uploadToBlob();
            } catch (Exception e) {
                addError("Failed to upload final logs", e);
            }
        }
        super.stop();
    }

    private void uploadToBlob() {
        if (logBuffer.size() > 0) {
            try {
                String blobName = resolveBlobName();
                BlobClient blobClient = containerClient.getBlobClient(blobName);

                // Append or create new blob
                byte[] content = logBuffer.toByteArray();
                if (blobClient.exists()) {
                    byte[] existingContent = downloadBlobContent(blobClient);
                    ByteArrayOutputStream combined = new ByteArrayOutputStream();
                    combined.write(existingContent);
                    combined.write(content);
                    content = combined.toByteArray();
                }

                blobClient.upload(new ByteArrayInputStream(content), content.length, true);
                logBuffer.reset();
            } catch (Exception e) {
                addError("Failed to upload logs to Azure Blob Storage", e);
            }
        }
    }

    private byte[] downloadBlobContent(BlobClient blobClient) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            addError("Failed to download existing blob content", e);
            return new byte[0];
        }
    }

    private String resolveBlobName() {
        String result = blobNamePattern;
        if (blobNamePattern.contains("%d")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateStr = LocalDate.now().format(formatter);
            result = blobNamePattern.replace("%d{yyyy-MM-dd}", dateStr);
        }
        return result;
    }
}
package com.tasos.demo.controller;


import com.tasos.demo.service.StorageBlobsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@RestController
@RequestMapping("/api/storage/blobs")
public class StorageBlobsController {

    public StorageBlobsController(StorageBlobsService storageBlobsService) {
        this.storageBlobsService = storageBlobsService;
    }

    private static final Logger logger = LoggerFactory.getLogger(StorageBlobsController.class);

    private final StorageBlobsService storageBlobsService;

    @GetMapping("/test")
    public String test() {
        return "Hello World: "+ storageBlobsService.test();
    }

    @GetMapping("/listContainers")
    public String listContainers() {
        return "listed Containers: "+ storageBlobsService.listContainers();
    }

    @GetMapping("/createContainer")
    public String createContainer(@RequestParam(required = false) String name) {
        logger.info("Creating container with name: {}", name);
        if (name == null || name.isEmpty()) {
            return "Container name cannot be null or empty.";
        }
        if (!name.matches("^[a-z0-9]+(-[a-z0-9]+)*$")) {
            return "Invalid container name format. It must be lowercase alphanumeric and can contain hyphens.";
        }
        if (!(name.equals("test") || name.equals("tasos"))) {
            return "Container name should be \"test\" or \"tasos\" (in order to prevent exessive number of new containers).";
        }
        return "New created container: "+ storageBlobsService.createUniqueContainer(name);
    }

    @GetMapping("/uploadTestFile")
    public String uploadTestFile(@RequestParam(required = false) String name) {
        logger.info("Uploading test file to container: {}", name);
        if (name == null || name.isEmpty()) {
            return "Container name cannot be null or empty.";
        }
        return "File uploaded to container: "+ storageBlobsService.uploadTestFileToContainer(name);
    }

    @GetMapping("/listFiles")
    public String listFiles(@RequestParam(required = false) String name) {
        logger.info("Listing files in container: {}", name);
        if (name == null || name.isEmpty()) {
            return "Container name cannot be null or empty.";
        }
        List<String> fileNames = storageBlobsService.listFilesInContainer(name);
        return (fileNames!=null ? ("Files in container: "+ fileNames) : ("Container "+ name+" does not exist."));
    }

    @GetMapping("/downloadBlobs")
    public ResponseEntity<byte[]> downloadBlobs(@RequestParam(required = false) String name) {
        logger.info("Downloading blobs from container: {}", name);
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body("Container name cannot be null or empty.".getBytes());
        }

        List<byte[]> blobsData = storageBlobsService.downloadBlobsFromContainer(name);

        if (blobsData.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<String> blobNames = storageBlobsService.listFilesInContainer(name);

        // Create ZIP file in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            for (int i = 0; i < Math.min(blobsData.size(), blobNames.size()); i++) {
                ZipEntry zipEntry = new ZipEntry(blobNames.get(i));
                zipOut.putNextEntry(zipEntry);
                zipOut.write(blobsData.get(i));
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            logger.error("Failed to create ZIP file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // Set headers for downloadable ZIP file
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", name + "-files.zip");

        return ResponseEntity.ok()
                .headers(headers)
                .body(baos.toByteArray());
    }

    @GetMapping("/deleteContainer")
    public String deleteContainer(@RequestParam(required = false) String name) {
        logger.info("Deleting container: {}", name);
        if (name == null || name.isEmpty()) {
            return "Container name cannot be null or empty.";
        }
        if (!(name.equals("test") || name.equals("tasos"))) {
            return "Container name should be \"test\" or \"tasos\" (in order to prevent deletion of other containers).";
        }
        String result = storageBlobsService.deleteContainer(name);
        return (result != null ? ("Container deleted: "+ result) : ("Container "+ name+" does not exist."));
    }

}
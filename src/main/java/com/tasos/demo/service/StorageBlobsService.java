package com.tasos.demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageBlobsService {

    boolean test();

    List<String> listContainers();

    String createUniqueContainer(String containerName);

    String uploadTestFileToContainer(String containerName);

    List<String> listFilesInContainer(String containerName);

    List<byte[]> downloadBlobsFromContainer(String containerName);

    String deleteContainer(String containerName);

    String uploadFileToContainer(String container, MultipartFile file);

    byte[] downloadFileFromContainer(String container, String fileName);

    String clearContainer(String container);

    String readContainerProperties(String containerName);

    String addContainerMetadata(String containerName);

}

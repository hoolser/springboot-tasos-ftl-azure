package com.tasos.demo.service;

import java.util.List;

public interface StorageBlobsService {

    boolean test();

    List<String> listContainers();

    String createUniqueContainer(String containerName);

    String uploadTestFileToContainer(String containerName);

    List<String> listFilesInContainer(String containerName);

    List<byte[]> downloadBlobsFromContainer(String containerName);

    String deleteContainer(String containerName);
}

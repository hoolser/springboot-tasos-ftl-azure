package com.tasos.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TasosApplication {

    @Value("${azure-storage-connection-string:not-set}")
    private String azureStorageConnectionStringFromPropertiesOrVault;

    public static void main(String[] args) {
        SpringApplication.run(TasosApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // This method is called after the application context is initialized
        // You can perform any initialization logic here if needed
        System.out.println("TasosApplication has been initialized successfully.");
        System.out.println("Active Spring Profiles: " + System.getProperty("spring.profiles.active"));
        System.out.println("azureStorageConnectionStringFromPropertiesOrVault: " + azureStorageConnectionStringFromPropertiesOrVault);
    }
}

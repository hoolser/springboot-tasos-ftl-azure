package com.tasos.demo.controller;

import com.tasos.demo.model.Product;
import com.tasos.demo.service.AzureCosmosDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cosmos")
public class AzureCosmosDbController {

    private static final Logger logger = LoggerFactory.getLogger(AzureCosmosDbController.class);
    private final AzureCosmosDbService azureCosmosDbService;

    public AzureCosmosDbController(AzureCosmosDbService azureCosmosDbService) {
        this.azureCosmosDbService = azureCosmosDbService;
    }

    @GetMapping("/create-sample")
    public String createSampleProduct(@RequestParam(required = false) String productName) {
        logger.info("Creating sample product in Cosmos DB with productName: {}", productName);
        if (productName == null || productName.isEmpty()) {
            return "productName cannot be null or empty.";
        }
        return azureCosmosDbService.connectAndCreateSampleProduct(productName);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        logger.info("Retrieving product with ID: {}", id);
        Product product = azureCosmosDbService.getProduct(id);

        if (product == null) {
            logger.warn("Product not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(product);
    }

    @GetMapping("/productsByName/{name}")
    public ResponseEntity<List<Product>> getProductsByName(@PathVariable String name) {
        logger.info("Retrieving products with Name: {}", name);
        List<Product> products = azureCosmosDbService.getProductsByName(name);

        if (products == null) {
            logger.warn("Products not found with name: {}", name);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(products);
    }
}
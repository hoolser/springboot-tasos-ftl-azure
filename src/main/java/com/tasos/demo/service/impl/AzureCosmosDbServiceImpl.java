package com.tasos.demo.service.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.tasos.demo.model.Product;
import com.tasos.demo.service.AzureCosmosDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AzureCosmosDbServiceImpl implements AzureCosmosDbService {

    private static final Logger logger = LoggerFactory.getLogger(AzureCosmosDbServiceImpl.class);

    @Value("${azure-cosmos-endpoint:}")
    private String cosmosDbEndpoint;

    @Value("${azure-cosmos-key:}")
    private String cosmosDbKey;

    @Value("${azure-cosmos-database:myDatabase}")
    private String databaseName;

    @Value("${azure-cosmos-container:myContainer}")
    private String containerName;

    @Value("${azure-cosmos-connection-enabled:false}")
    private boolean connectionEnabled;

    private CosmosClient cosmosClient;
    private CosmosDatabase database;
    private CosmosContainer container;

    @PostConstruct
    public void initialize() {
        if(connectionEnabled){
            logger.info("Initializing Cosmos DB client");
            this.cosmosClient = new CosmosClientBuilder()
                    .endpoint(cosmosDbEndpoint)
                    .key(cosmosDbKey)
                    .buildClient();
        }

    }

    @Override
    public String connectAndCreateSampleProduct(String productName) {
        try {
            // Create database if it doesn't exist
            CosmosDatabaseResponse databaseResponse = cosmosClient.createDatabaseIfNotExists(databaseName);
            database = cosmosClient.getDatabase(databaseResponse.getProperties().getId());
            logger.info("Database {} is ready", database.getId());

            // Create container with partition key - without throughput for serverless accounts
            CosmosContainerProperties containerProperties = new CosmosContainerProperties(containerName, "/name");
            CosmosContainerResponse containerResponse = database.createContainerIfNotExists(containerProperties);
            container = database.getContainer(containerResponse.getProperties().getId());
            logger.info("Container {} is ready", container.getId());

            // Create a product
            String id = UUID.randomUUID().toString();
            Product product = new Product(id, productName, "This is a sample product created from Java SDK");

            // Add the item to the container
            CosmosItemResponse<Product> itemResponse =
                    container.createItem(product, new PartitionKey(product.getName()), null);

            // Add null check before accessing the item
            if (itemResponse.getItem() != null) {
                logger.info("Created product with id {}, operation consumed {} RUs",
                        itemResponse.getItem().getId(), itemResponse.getRequestCharge());
            } else {
                logger.info("Created product, operation consumed {} RUs",
                        itemResponse.getRequestCharge());
            }
            return "Successfully created product with ID: " + id;
        } catch (Exception e) {
            logger.error("Error connecting to Cosmos DB", e);
            return "Error: " + e.getMessage();
        }
    }

    @Override
    public Product getProduct(String id) {
        try {
            if (container == null) {
                database = cosmosClient.getDatabase(databaseName);
                container = database.getContainer(containerName);
            }

            // First query to get the product name for partition key
            Product product = container.queryItems(
                            "SELECT * FROM c WHERE c.id = '" + id + "'",
                            new CosmosQueryRequestOptions(),
                            Product.class)
                    .stream().findFirst().orElse(null);

            if (product != null) {
                // Now read with correct partition key
                return container.readItem(id, new PartitionKey(product.getName()), Product.class).getItem();
            }
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving product with id {}", id, e);
            return null;
        }
    }

    @Override
    public List<Product> getProductsByName(String name) {
        try {
            if (container == null) {
                database = cosmosClient.getDatabase(databaseName);
                container = database.getContainer(containerName);
            }

            // Query to get all products with the given name
            return container.queryItems(
                            "SELECT * FROM c WHERE c.name = '" + name + "'",
                            new CosmosQueryRequestOptions(),
                            Product.class)
                    .stream()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error retrieving products with name {}", name, e);
            return Collections.emptyList();
        }
    }


}
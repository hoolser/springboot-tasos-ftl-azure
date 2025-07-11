package com.tasos.demo.service;

import com.tasos.demo.model.Product;

import java.util.List;

public interface AzureCosmosDbService {

    String connectAndCreateSampleProduct(String productName);

    Product getProduct(String id);

    List<Product> getProductsByName(String name);

}

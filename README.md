# Spring Boot Tasos FTL Azure

This project is a sample **Spring Boot** application demonstrating the use of [Freemarker](https://freemarker.apache.org/) templates (`.ftl`) together with Azure deployment. It provides a simple web application that renders dynamic HTML pages via Freemarker.

## Features

- **Spring Boot** backend
- **Freemarker Template Engine** for dynamic page rendering
- **Azure App Service** deployment support
- Simple example of controller + template integration

## Getting Started

These instructions will help you build, run, and deploy the application.

### Prerequisites

- Java 17+ installed
- Maven 3.x installed
- Azure CLI (optional, for deployment)

### Running Locally

Clone the repository:

git clone https://github.com/hoolser/springboot-tasos-ftl-azure.git
cd springboot-tasos-ftl-azure

Build the project:

mvn clean package

Run the application:

mvn spring-boot:run

The application will start at:

http://localhost:8080

Available Endpoints

/ – Root endpoint, returns the main Freemarker template.

/hello – Example endpoint rendering a greeting.


Building a JAR

You can build an executable JAR:

mvn clean package

The JAR will be in target/springboot-tasos-ftl-azure-*.jar.

Run it with:

java -jar target/springboot-tasos-ftl-azure-*.jar

Deploying to Azure

To deploy to Azure App Service, follow these general steps:

1. Create an Azure App Service.


2. Deploy your JAR or configure continuous deployment from GitHub.


3. Ensure JAVA_VERSION in Azure is set appropriately (Java 17+).



You can also use Azure CLI or Maven plugins to automate deployment.

Project Structure

src
 └── main
      ├── java
      │    └── com.example.demo
      │          └── [Controllers, Main Application]
      └── resources
           ├── templates
           │      └── [Freemarker .ftl files]
           └── application.properties

DemoApplication.java: Spring Boot main class.

templates: Freemarker templates rendered by the controller.


Contributing

Contributions are welcome! Please open an issue or submit a pull request if you have improvements.

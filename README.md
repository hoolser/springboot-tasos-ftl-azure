```markdown
# 🌱 Spring Boot Tasos FTL Azure

```

```markdown
A sample **Spring Boot** application demonstrating [Freemarker](https://freemarker.apache.org/) templates (`.ftl`) and Azure deployment. This simple web app renders dynamic HTML pages using Freemarker.
```

```markdown
## 🚀 Features

- **Spring Boot** backend
- **Freemarker Template Engine** for dynamic page rendering
- **Azure App Service** deployment support
- Example: controller + template integration
```

```markdown
## 🏁 Getting Started

Follow these steps to build, run, and deploy the application.
```

```markdown
### Prerequisites

- Java 17+
- Maven 3.x
- Azure CLI _(optional, for deployment)_
```

```markdown
### Configuration

#### Azure Storage Connection String

This app requires an Azure Storage account connection string:

1. **Local Development**: Set the connection string as an environment variable:

   - **Windows**
     ```
     set AZURE_STORAGE_CONNECTION_STRING=your_connection_string_here
     ```
   - **Linux/macOS**
     ```
     export AZURE_STORAGE_CONNECTION_STRING=your_connection_string_here
     ```

2. **Azure Deployment**: Store the connection string in Azure Key Vault or as an App Service Configuration setting:
   - In App Service Configuration: Add application setting `azure-storage-connection-string`
   - In Azure Key Vault: Store the secret and configure your app to access it

> ⚠️ Without this configuration, all storage-related functionality will fail.
```

```markdown
#### File Upload Limits

The application enforces two file size limits:

- **Spring Boot global limit**: Set in `application.properties`

  spring.servlet.multipart.max-file-size=100MB
  spring.servlet.multipart.max-request-size=100MB
  ```

  ```
- **Application container size limit**: Set in the service implementation (default: 100MB)
```

```markdown
### 🖥️ Running Locally

Clone the repository:

git clone https://github.com/hoolser/springboot-tasos-ftl-azure.git
cd springboot-tasos-ftl-azure
```

```

Build the project:

mvn clean package
```

```

Run the application:

mvn spring-boot:run
```

```

The app will start at: [http://localhost:8080]
```

```markdown
#### Available Endpoints

- `/` – Root endpoint, returns the main Freemarker template
```

```markdown
### 🏗️ Building a JAR

You can build an executable JAR:

mvn clean package
```

```
The JAR will be in `target/springboot-tasos-ftl-azure-*.jar`.
```
```
Run it with:

java -jar target/springboot-tasos-ftl-azure-*.jar
```

```markdown
### ☁️ Deploying to Azure

To deploy to Azure App Service:

1. Create an Azure App Service.
2. Deploy your JAR or configure continuous deployment from GitHub.
3. Ensure `JAVA_VERSION` in Azure is set to Java 17.

You can also use Azure CLI or Maven plugins to automate deployment.
```

````
## 📁 Project Structure
src
 └── main
      ├── java
      │    └── com.example.demo
      │          └── [Controllers, Main Application]
      └── resources
           ├── templates
           │      └── [Freemarker .ftl files]
           └── application.properties
````           
```
- `DemoApplication.java`: Spring Boot main class
- `templates`: Freemarker templates rendered by the controller
```

```markdown
## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a pull request for improvements.
```
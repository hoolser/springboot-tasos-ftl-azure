<!DOCTYPE html>
<html>
<head>
    <title>Spring Boot FreeMarker Landing Page</title>
    <link rel="stylesheet" href="/css/styles.css">
    <link rel="icon" type="image/x-icon" href="/images/favicon.ico">
</head>
<body>
<div class="centered-message">
    <h1>Welcome to the Spring Boot Demo!</h1>
    <p>${message}</p>
    <a href="/storage-blob-page">
        <button class="home-button" style="margin-top: 30px;">Go to Storage Blob Management</button>
    </a>
</div>
</body>
</html>
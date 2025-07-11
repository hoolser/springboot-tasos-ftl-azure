<!DOCTYPE html>
<html>
<head>
    <title>Spring Boot FreeMarker Landing Page</title>
    <link rel="stylesheet" href="/css/styles.css">
    <link rel="icon" type="image/x-icon" href="/favicon.ico">
</head>
<body>
<div class="centered-message">
    <h1>Welcome to the Spring Boot Demo!</h1>
    <p>${message}</p>
    <div class="home-buttons">
        <a href="/storage-blob-page">
            <button class="home-button" style="margin-top: 30px;">Go to Storage Blob Management</button>
        </a>
        <a href="/share-blob-page">
            <button class="home-button" style="margin-top: 30px;">Go to Share Blob Page</button>
        </a>
    </div>
</div>
</body>
</html>
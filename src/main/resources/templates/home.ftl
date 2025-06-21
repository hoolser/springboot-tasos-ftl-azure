<!DOCTYPE html>
<html>
<head>
    <title>Storage Blobs Management</title>
    <link rel="stylesheet" href="/css/styles.css">
    <link rel="icon" type="image/x-icon" href="/images/favicon.ico">
    <script src="/js/home.js"></script>
</head>
<body class="home-page">
<h2>${message}</h2>
<h1>Storage Blobs Management</h1>

<div class="home-container">
    <div class="home-form-group">
        <label class="home-label" for="containerName">Container Name:</label>
        <input class="home-input" type="text" id="containerName" name="containerName" placeholder="Enter container name">
    </div>

    <div class="home-buttons">
<#--        <button class="home-button" onclick="callApi('test')">Test Connection</button>-->
        <button class="home-button" onclick="callApi('listContainers')">List Containers</button>
        <button class="home-button" onclick="callApi('createContainer')">Create Container</button>
        <button class="home-button" onclick="callApi('uploadTestFile')">Upload Test File</button>
        <button class="home-button" onclick="callApi('listFiles')">List Files</button>
        <button class="home-button" onclick="callApi('downloadBlobs')">Download Blobs</button>
        <button class="home-button" onclick="callApi('deleteContainer')">Delete Container</button>
    </div>
</div>

<div class="home-container">
    <h2 class="home-title">Response:</h2>
    <div id="response" class="home-response">
        <p>Results will appear here...</p>
    </div>
</div>

</body>
</html>
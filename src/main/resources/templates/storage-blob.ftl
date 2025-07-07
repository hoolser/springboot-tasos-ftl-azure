<!DOCTYPE html>
<html>
<head>
  <title>Storage Blobs Management</title>
  <link rel="stylesheet" href="/css/styles.css">
  <link rel="icon" type="image/x-icon" href="/favicon.ico">
  <script src="/js/storage-blob.js"></script>
</head>
<body class="storage-page">

<div style="text-align: left; margin-bottom: 20px;">
  <a href="/">
    <button class="home-button">&#8592; Back to Home Page</button>
  </a>
</div>

<h2>${message}</h2>
<h1>Storage Blobs Management</h1>

<div class="storage-container">
  <div class="storage-form-group">
    <label class="storage-label" for="containerName">Container Name:</label>
    <input class="storage-input" type="text" id="containerName" name="containerName" placeholder="Enter container name">
  </div>

  <div class="storage-buttons">
      <#--        <button class="storage-button" onclick="callApi('test')">Test Connection</button>-->
    <button class="storage-button" onclick="callApi('listContainers')">List Containers</button>
    <button class="storage-button" onclick="callApi('createContainer')">Create Container</button>
    <button class="storage-button" onclick="callApi('uploadTestFile')">Upload Test File</button>
    <button class="storage-button" onclick="callApi('listFiles')">List Files</button>
    <button class="storage-button" onclick="callApi('downloadBlobs')">Download Blobs</button>
    <button class="storage-button" onclick="callApi('deleteContainer')">Delete Container</button>
  </div>
</div>

<div class="storage-container">
  <h2 class="storage-title">Response:</h2>
  <div id="response" class="storage-response">
    <p>Results will appear here...</p>
  </div>
</div>

</body>
</html>
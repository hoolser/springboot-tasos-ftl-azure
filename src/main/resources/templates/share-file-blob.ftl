<!DOCTYPE html>
<html>
<head>
  <title>Share File Blob</title>
  <link rel="stylesheet" href="/css/styles.css">
  <link rel="icon" type="image/x-icon" href="/images/favicon.ico">
  <script src="/js/share-file-blob.js"></script>
</head>
<body class="storage-page">

<div style="text-align: left; margin-bottom: 20px;">
  <a href="/">
    <button class="home-button">&#8592; Back to Home Page</button>
  </a>
</div>

<h1>Share File Blob</h1>

<div class="storage-container">
  <h2 class="storage-title">Upload a File to Shared Container</h2>
  <form id="uploadForm" method="post" enctype="multipart/form-data" action="/api/storage/blobs/share/upload">
    <div class="storage-form-group">
      <input class="storage-input" type="file" name="file" required>
    </div>
    <button class="storage-button" type="submit">Upload</button>
  </form>
</div>

<div class="storage-container">
  <h2 class="storage-title">List Files in Shared Container</h2>
  <button class="storage-button" type="button" id="listFilesButton">List Files</button>
</div>

<div class="storage-container">
  <h2 class="storage-title">Download a File from Shared Container</h2>
  <form id="downloadForm" method="get" action="/api/storage/blobs/share/download">
    <div class="storage-form-group">
      <input class="storage-input" type="text" id="downloadFileName" name="fileName" placeholder="Enter file name" required>
    </div>
    <button class="storage-button" type="submit">Download</button>
  </form>
</div>

<div class="storage-container">
  <h2 class="storage-title">Response:</h2>
  <div id="responseMessage" class="storage-response">
    <p>Results will appear here...</p>
  </div>
</div>

<div class="storage-container">
  <h2 class="storage-title">Share Download Link</h2>
  <div class="storage-form-group">
    <label for="shareFileName">File name to share:</label>
    <input class="storage-input" type="text" id="shareFileName" placeholder="Enter file name to share" style="width: 80%;">
  </div>
  <div class="storage-form-group">
    <label for="shareLink">Generated share link:</label>
    <input class="storage-input" type="text" id="shareLink" readonly style="width: 80%; background-color: #f0f0f0;">
  </div>
  <button class="storage-button" type="button" onclick="generateShareLink()">Generate Share Link</button>
</div>

<div class="storage-container">
  <h2 class="storage-title">Clear Shared Container</h2>
  <button class="storage-button" type="button" id="clearButton">Clear All Files</button>
</div>

</body>
</html>
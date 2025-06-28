<!DOCTYPE html>
<html>
<head>
  <title>Share File Blob</title>
  <link rel="stylesheet" href="/css/styles.css">
  <link rel="icon" type="image/x-icon" href="/images/favicon.ico">
  <script>
      function generateShareLink() {
          const fileName = document.getElementById('downloadFileName').value;
          if (fileName) {
              const link = window.location.origin + '/api/storage/blobs/share/download?fileName=' + encodeURIComponent(fileName);
              document.getElementById('shareLink').value = link;
          } else {
              document.getElementById('shareLink').value = 'Enter a file name first.';
          }
      }

      window.addEventListener('DOMContentLoaded', function() {
          // Upload form AJAX
          document.getElementById('uploadForm').addEventListener('submit', function(e) {
              e.preventDefault();
              const formData = new FormData(this);
              fetch('/api/storage/blobs/share/upload', {
                  method: 'POST',
                  body: formData
              })
                  .then(response => response.text())
                  .then(data => {
                      document.getElementById('responseMessage').textContent = data;
                  })
                  .catch(() => {
                      document.getElementById('responseMessage').textContent = 'Upload failed.';
                  });
          });

          // Download form AJAX
          document.getElementById('downloadForm').addEventListener('submit', function(e) {
              e.preventDefault();
              const fileName = document.getElementById('downloadFileName').value;
              fetch('/api/storage/blobs/share/download?fileName=' + encodeURIComponent(fileName))
                  .then(response => {
                      if (response.ok) {
                          return response.blob().then(blob => {
                              // Download the file
                              const url = window.URL.createObjectURL(blob);
                              const a = document.createElement('a');
                              a.href = url;
                              a.download = fileName;
                              document.body.appendChild(a);
                              a.click();
                              a.remove();
                              window.URL.revokeObjectURL(url);
                              document.getElementById('responseMessage').textContent = 'Download started.';
                          });
                      } else {
                          document.getElementById('responseMessage').textContent = 'File not found or download failed.';
                      }
                  })
                  .catch(() => {
                      document.getElementById('responseMessage').textContent = 'Download failed.';
                  });
          });

          document.getElementById('clearButton').addEventListener('click', function() {
              if (confirm('Are you sure you want to delete all files in the shared container?')) {
                  fetch('/api/storage/blobs/share/clear', { method: 'POST' })
                      .then(response => response.text())
                      .then(data => {
                          document.getElementById('responseMessage').textContent = data;
                      })
                      .catch(() => {
                          document.getElementById('responseMessage').textContent = 'Clear action failed.';
                      });
              }
          });
      });
  </script>
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
  <h2 class="storage-title">Download a File from Shared Container</h2>
  <form id="downloadForm" method="get" action="/api/storage/blobs/share/download">
    <div class="storage-form-group">
      <input class="storage-input" type="text" id="downloadFileName" name="fileName" placeholder="Enter file name" required>
    </div>
    <button class="storage-button" type="submit">Download</button>
  </form>
</div>

<div class="storage-container">
  <h2 class="storage-title">Share Download Link</h2>
  <div class="storage-form-group">
    <input class="storage-input" type="text" id="shareLink" readonly style="width: 80%;">
  </div>
  <button class="storage-button" type="button" onclick="generateShareLink()">Generate Share Link</button>
</div>

<div class="storage-container">
  <h2 class="storage-title">Response:</h2>
  <div id="responseMessage" class="storage-response">
    <p>Results will appear here...</p>
  </div>
</div>

<div class="storage-container">
  <h2 class="storage-title">Clear Shared Container</h2>
  <button class="storage-button" type="button" id="clearButton">Clear All Files</button>
</div>

</body>
</html>
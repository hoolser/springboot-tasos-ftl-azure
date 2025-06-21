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

<#--<script>
    function callApi(endpoint) {
        const containerName = document.getElementById('containerName').value;
        const responseDiv = document.getElementById('response');

        // For download operation, handle differently
        if (endpoint === 'downloadBlobs') {
            if (!containerName) {
                responseDiv.innerHTML = '<p>Error: Container name is required</p>';
                return;
            }
            window.location.href = `/api/storage/blobs/${endpoint}?name=${encodeURIComponent(containerName)}`;
            responseDiv.innerHTML = '<p>Download initiated...</p>';
            return;
        }

        // For other operations
        let url = `/api/storage/blobs/${endpoint}`;

        // Add name parameter if needed
        if (['createContainer', 'uploadTestFile', 'listFiles', 'deleteContainer'].includes(endpoint)) {
            if (!containerName) {
                responseDiv.innerHTML = '<p>Error: Container name is required</p>';
                return;
            }
            url += `?name=${encodeURIComponent(containerName)}`;
        }

        // Show loading state
        responseDiv.innerHTML = '<p>Loading...</p>';

        // Make the API call
        fetch(url)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! Status: ${response.status}`);
                    }
                    return response.text();
                })
                .then(data => {
                    responseDiv.innerHTML = `<p>${data}</p>`;
                })
                .catch(error => {
                    responseDiv.innerHTML = `<p>Error: ${error.message}</p>`;
                });
    }
</script>-->
</body>
</html>
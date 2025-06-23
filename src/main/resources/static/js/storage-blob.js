document.addEventListener('DOMContentLoaded', function() {
    // Make callApi function available globally
    window.callApi = function(endpoint) {
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
    };
});
function generateShareLink() {
    const fileName = document.getElementById('shareFileName').value;
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

    document.getElementById('listFilesButton').addEventListener('click', function() {
        fetch('/api/storage/blobs/listFilesOfShared')
            .then(response => response.text())
            .then(data => {
                document.getElementById('responseMessage').textContent = data;
            })
            .catch(() => {
                document.getElementById('responseMessage').textContent = 'Failed to list files.';
            });
    });
});
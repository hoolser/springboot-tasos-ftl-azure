<!DOCTYPE html>
<html>
<head>
  <title>SRT Subtitle Translation</title>
  <link rel="stylesheet" href="/css/styles.css">
  <link rel="icon" type="image/x-icon" href="/favicon.ico">
  <style>
    .srt-translation-container {
      max-width: 900px;
      margin: 30px auto;
      padding: 20px;
      background-color: #f5f5f5;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .srt-header {
      text-align: center;
      margin-bottom: 30px;
      color: #333;
    }

    .srt-header h1 {
      margin: 0 0 10px 0;
      color: #2c3e50;
    }

    .srt-header p {
      margin: 5px 0;
      color: #666;
      font-size: 14px;
    }

    .srt-upload-section {
      background-color: white;
      padding: 20px;
      border-radius: 6px;
      margin-bottom: 20px;
      border: 2px dashed #3498db;
    }

    .file-input-wrapper {
      position: relative;
      display: inline-block;
      width: 100%;
    }

    .file-input-label {
      display: block;
      padding: 40px;
      text-align: center;
      background-color: #ecf0f1;
      border-radius: 6px;
      cursor: pointer;
      transition: background-color 0.3s;
    }

    .file-input-label:hover {
      background-color: #d5dbdb;
    }

    #srtFileInput {
      display: none;
    }

    .file-selected-info {
      margin-top: 15px;
      padding: 10px;
      background-color: #d5f4e6;
      border-left: 4px solid #27ae60;
      border-radius: 3px;
      display: none;
    }

    .file-selected-info.show {
      display: block;
    }

    .file-selected-info .file-name {
      font-weight: bold;
      color: #27ae60;
    }

    .file-selected-info .file-size {
      font-size: 13px;
      color: #555;
      margin-top: 5px;
    }

    .action-buttons {
      display: flex;
      gap: 10px;
      margin-top: 15px;
      justify-content: center;
    }

    .btn {
      padding: 10px 20px;
      border: none;
      border-radius: 5px;
      cursor: pointer;
      font-size: 14px;
      font-weight: bold;
      transition: all 0.3s;
    }

    .btn-translate {
      background-color: #27ae60;
      color: white;
    }

    .btn-translate:hover:not(:disabled) {
      background-color: #229954;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }

    .btn-clear {
      background-color: #e74c3c;
      color: white;
    }

    .btn-clear:hover:not(:disabled) {
      background-color: #c0392b;
    }

    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .progress-section {
      background-color: white;
      padding: 20px;
      border-radius: 6px;
      margin-bottom: 20px;
      display: none;
    }

    .progress-section.show {
      display: block;
    }

    .progress-bar {
      width: 100%;
      height: 30px;
      background-color: #ecf0f1;
      border-radius: 5px;
      overflow: hidden;
      margin-bottom: 10px;
    }

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #3498db, #2980b9);
      width: 0%;
      transition: width 0.3s;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 12px;
      font-weight: bold;
    }

    .progress-text {
      font-size: 14px;
      color: #555;
    }

    .response-section {
      background-color: white;
      padding: 20px;
      border-radius: 6px;
      margin-bottom: 20px;
    }

    .response-section h3 {
      margin-top: 0;
      color: #2c3e50;
      border-bottom: 2px solid #3498db;
      padding-bottom: 10px;
    }

    .response-content {
      max-height: 300px;
      overflow-y: auto;
      padding: 10px;
      background-color: #f8f9fa;
      border-radius: 4px;
      font-family: 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.5;
      white-space: pre-wrap;
      word-wrap: break-word;
    }

    .response-content.success {
      border-left: 4px solid #27ae60;
      color: #27ae60;
    }

    .response-content.error {
      border-left: 4px solid #e74c3c;
      color: #e74c3c;
    }

    .response-content.info {
      border-left: 4px solid #3498db;
      color: #2c3e50;
    }

    .translations-list-section {
      background-color: white;
      padding: 20px;
      border-radius: 6px;
    }

    .translations-list-section h3 {
      margin-top: 0;
      color: #2c3e50;
      border-bottom: 2px solid #3498db;
      padding-bottom: 10px;
    }

    .translation-item {
      padding: 10px;
      margin: 5px 0;
      background-color: #f8f9fa;
      border-radius: 4px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-left: 3px solid #3498db;
    }

    .translation-item .file-name-text {
      flex: 1;
      font-weight: 500;
      color: #2c3e50;
      word-break: break-all;
    }

    .btn-download {
      background-color: #3498db;
      color: white;
      padding: 8px 15px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
      font-weight: bold;
      margin-left: 10px;
      border: none;
      transition: all 0.3s;
    }

    .btn-download:hover {
      background-color: #2980b9;
    }

    .spinner {
      display: inline-block;
      width: 20px;
      height: 20px;
      border: 3px solid #f3f3f3;
      border-top: 3px solid #3498db;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-right: 10px;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .info-box {
      background-color: #e8f4f8;
      border-left: 4px solid #3498db;
      padding: 15px;
      border-radius: 4px;
      margin-bottom: 20px;
      font-size: 14px;
      color: #2c3e50;
    }

    .info-box strong {
      display: block;
      margin-bottom: 5px;
    }

    .error-message {
      background-color: #fadbd8;
      border-left: 4px solid #e74c3c;
      padding: 10px;
      border-radius: 4px;
      color: #c0392b;
      margin: 10px 0;
    }

    .back-button {
      text-align: left;
      margin-bottom: 20px;
    }

    .back-button a {
      display: inline-block;
      padding: 10px 15px;
      background-color: #34495e;
      color: white;
      text-decoration: none;
      border-radius: 5px;
      transition: background-color 0.3s;
    }

    .back-button a:hover {
      background-color: #2c3e50;
    }
  </style>
</head>
<body>

<div class="back-button">
  <a href="/">← Back to Home Page</a>
</div>

<div class="srt-translation-container">
  <div class="srt-header">
    <h1>🎬 SRT Subtitle Translation</h1>
    <p>English (EN) → Greek (EL) Translation</p>
    <p>Powered by <span id="translationProvider">DEEPL</span> API</p>
  </div>

  <div class="info-box">
    <strong>Instructions:</strong>
    <ul style="margin: 10px 0; padding-left: 20px;">
      <li>Upload an SRT subtitle file (English subtitles)</li>
      <li>The file will be translated to Greek using the selected provider</li>
      <li>HTML tags (like &lt;i&gt;, &lt;b&gt;) are preserved</li>
      <li>Maximum file size: <span id="maxFileSize">50</span> MB</li>
    </ul>
  </div>

  <div class="provider-selection" style="background-color: white; padding: 15px; border-radius: 6px; margin-bottom: 20px; border: 1px solid #ddd;">
      <h3 style="margin-top: 0; color: #2c3e50; font-size: 16px; margin-bottom: 10px;">Translation Provider</h3>
      <div class="radio-group" style="display: flex; gap: 20px;">
          <label style="cursor: pointer; display: flex; align-items: center;">
              <input type="radio" name="provider" value="deepl" checked style="margin-right: 8px;">
              <strong>DeepL</strong>
          </label>
          <label style="cursor: pointer; display: flex; align-items: center;">
              <input type="radio" name="provider" value="azure" style="margin-right: 8px;">
              <strong>Azure Translator</strong>
          </label>
      </div>
  </div>

  <!-- Upload Section -->
  <div class="srt-upload-section">
    <div class="file-input-wrapper">
      <label class="file-input-label" for="srtFileInput">
        <div>
          <span style="font-size: 30px;">📁</span>
          <p style="margin: 10px 0; color: #333;">
            Click to select SRT file or drag and drop here
          </p>
          <p style="margin: 5px 0; color: #999; font-size: 12px;">
            Supported format: .srt
          </p>
        </div>
      </label>
      <input type="file" id="srtFileInput" accept=".srt" />
    </div>

    <div class="file-selected-info" id="fileSelectedInfo">
      <span class="file-name" id="selectedFileName"></span>
      <div class="file-size" id="selectedFileSize"></div>
    </div>

    <div class="action-buttons">
      <button class="btn btn-translate" id="translateBtn" onclick="startTranslation()" disabled>
        Translate to Greek
      </button>
      <button class="btn btn-clear" id="clearBtn" onclick="clearFileSelection()">
        Clear Selection
      </button>
    </div>
  </div>

  <!-- Progress Section -->
  <div class="progress-section" id="progressSection">
    <div class="progress-bar">
      <div class="progress-fill" id="progressFill">0%</div>
    </div>
    <div class="progress-text">
      <span class="spinner"></span>
      <span id="progressText">Processing your file...</span>
    </div>
  </div>

  <!-- Response Section -->
  <div class="response-section" id="responseSection" style="display: none;">
    <h3>Translation Result</h3>
    <div class="response-content" id="responseContent"></div>
  </div>

  <!-- Translations List Section -->
  <div class="translations-list-section" id="translationsListSection" style="display: none;">
    <h3>Your Translated Files</h3>
    <div id="translationsList"></div>
    <div style="text-align: center; margin-top: 15px;">
      <button class="btn btn-translate" onclick="refreshTranslationsList()">
        Refresh List
      </button>
    </div>
  </div>
</div>

<script>
  let selectedFile = null;

  // Drag and drop functionality
  const fileInputLabel = document.querySelector('.file-input-label');
  const fileInput = document.getElementById('srtFileInput');

  fileInputLabel.addEventListener('dragover', (e) => {
    e.preventDefault();
    fileInputLabel.style.backgroundColor = '#d5dbdb';
  });

  fileInputLabel.addEventListener('dragleave', () => {
    fileInputLabel.style.backgroundColor = '#ecf0f1';
  });

  fileInputLabel.addEventListener('drop', (e) => {
    e.preventDefault();
    fileInputLabel.style.backgroundColor = '#ecf0f1';

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFileSelect(files[0]);
    }
  });

  fileInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) {
      handleFileSelect(e.target.files[0]);
    }
  });

  function handleFileSelect(file) {
    if (!file.name.toLowerCase().endsWith('.srt')) {
      showErrorMessage('Please select a valid .srt file');
      return;
    }

    selectedFile = file;
    document.getElementById('selectedFileName').textContent = file.name;
    document.getElementById('selectedFileSize').textContent =
      'Size: ' + formatFileSize(file.size);
    document.getElementById('fileSelectedInfo').classList.add('show');
    document.getElementById('translateBtn').disabled = false;
  }

  function clearFileSelection() {
    selectedFile = null;
    fileInput.value = '';
    document.getElementById('fileSelectedInfo').classList.remove('show');
    document.getElementById('translateBtn').disabled = true;
    document.getElementById('responseSection').style.display = 'none';
  }

  function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  function startTranslation() {
    if (!selectedFile) {
      showErrorMessage('Please select a file first');
      return;
    }

    const provider = document.querySelector('input[name="provider"]:checked').value;

    const formData = new FormData();
    formData.append('file', selectedFile);

    // Show progress section
    document.getElementById('progressSection').classList.add('show');
    document.getElementById('responseSection').style.display = 'none';
    document.getElementById('translateBtn').disabled = true;

    // Simulate progress
    let progress = 0;
    const progressInterval = setInterval(() => {
      progress += Math.random() * 30;
      if (progress > 90) progress = 90;
      updateProgress(progress);
    }, 500);

    // Send translation request
    fetch('/api/srt/translation/translateEnToEl?provider=' + encodeURIComponent(provider), {
      method: 'POST',
      body: formData
    })
    .then(response => {
      clearInterval(progressInterval);
      updateProgress(100);

      if (!response.ok) {
        return response.text().then(text => {
          throw new Error(text);
        });
      }
      return response.text();
    })
    .then(data => {
      setTimeout(() => {
        document.getElementById('progressSection').classList.remove('show');
        showSuccessResponse(data);
        document.getElementById('translateBtn').disabled = false;
        refreshTranslationsList();
      }, 500);
    })
    .catch(error => {
      clearInterval(progressInterval);
      document.getElementById('progressSection').classList.remove('show');
      showErrorResponse('Translation failed: ' + error.message);
      document.getElementById('translateBtn').disabled = false;
    });
  }

  function updateProgress(percent) {
    const progressFill = document.getElementById('progressFill');
    progressFill.style.width = percent + '%';
    progressFill.textContent = Math.round(percent) + '%';
  }

  function showSuccessResponse(message) {
    const responseSection = document.getElementById('responseSection');
    const responseContent = document.getElementById('responseContent');
    responseContent.className = 'response-content success';
    responseContent.textContent = '✓ ' + message;
    responseSection.style.display = 'block';
  }

  function showErrorResponse(message) {
    const responseSection = document.getElementById('responseSection');
    const responseContent = document.getElementById('responseContent');
    responseContent.className = 'response-content error';
    responseContent.textContent = '✗ ' + message;
    responseSection.style.display = 'block';
  }

  function showErrorMessage(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    document.querySelector('.srt-upload-section').insertBefore(
      errorDiv,
      document.querySelector('.action-buttons')
    );
    setTimeout(() => {
      errorDiv.remove();
    }, 5000);
  }

  function refreshTranslationsList() {
    fetch('/api/srt/translation/listTranslations')
    .then(response => response.text())
    .then(data => {
      displayTranslationsList(data);
    })
    .catch(error => {
      console.error('Error fetching translations list:', error);
    });
  }

  function loadActiveProvider() {
    fetch('/api/srt/translation/provider')
    .then(response => response.text())
    .then(data => {
      const match = data.match(/:\s*(\w+)/);
      if (match) {
        const providerName = match[1].toLowerCase();
        document.getElementById('translationProvider').textContent = providerName.toUpperCase();

        // Update radio button selection
        const radio = document.querySelector('input[name="provider"][value="' + providerName + '"]');
        if (radio) {
            radio.checked = true;
        }
      }
    })
    .catch(error => console.error('Error loading translation provider:', error));
  }

  function displayTranslationsList(data) {
    const listSection = document.getElementById('translationsListSection');
    const list = document.getElementById('translationsList');

    if (data.includes('No translated')) {
      list.innerHTML = '<p style="color: #999;">No translated files yet.</p>';
      listSection.style.display = 'block';
      return;
    }

    const files = data.split('\n').filter(line => line.includes('translated_en_el_'));

    if (files.length === 0) {
      list.innerHTML = '<p style="color: #999;">No translated files yet.</p>';
      listSection.style.display = 'block';
      return;
    }

    list.innerHTML = files.map(file => {
      const fileName = file.replace(/^\s*-\s*/, '').trim();
      if (!fileName) return '';

      return `
        <div class="translation-item">
          <span class="file-name-text">${'$'}{fileName}</span>
          <button class="btn-download" onclick="downloadFile('${r'${fileName}'}')">
            Download
          </button>
        </div>
      `;
    }).join('');

    listSection.style.display = 'block';
  }

  function downloadFile(fileName) {
    window.location.href = '/api/srt/translation/download?fileName=' + encodeURIComponent(fileName);
  }

  // Load max file size on page load
  window.addEventListener('load', () => {
    fetch('/api/srt/translation/maxFileSize')
    .then(response => response.text())
    .then(data => {
      const match = data.match(/(\d+)\s*MB/);
      if (match) {
        document.getElementById('maxFileSize').textContent = match[1];
      }
    })
    .catch(error => console.error('Error loading max file size:', error));

    loadActiveProvider();
    refreshTranslationsList();
  });
</script>

</body>
</html>


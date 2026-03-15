# SRT Subtitle Translation Feature

## Overview
This feature allows users to upload SRT subtitle files in English and automatically translate them to Greek using the DeepL API. The translated files are saved to the shared storage container.

## Features

- **SRT File Validation**: Validates that uploaded files are in valid SRT format
- **Size Limit Enforcement**: Enforces maximum file size limits (50 MB default)
- **HTML Tag Preservation**: Preserves HTML/XML tags (e.g., `<i>`, `<b>`) in the translated text
- **Optimized Translation**: Sends only the minimum necessary text to the DeepL API
  - Removes HTML tags before sending to API
  - Concatenates multi-line subtitles into single lines
  - Skips index lines and blank lines
- **Automatic File Naming**: Translated files are saved with timestamp and naming convention
- **Download Functionality**: Users can download translated files from the shared container
- **Progress Tracking**: UI shows translation progress

## File Structure

### New Files Created

1. **SrtSubtitle.java** (`model/SrtSubtitle.java`)
   - Model class representing a single subtitle entry with index, timecode, and text

2. **SrtParser.java** (`util/SrtParser.java`)
   - Utility class for parsing SRT format
   - Methods: `parse()`, `removeHtmlTags()`, `isSrtFile()`

3. **SrtTranslationService.java** (`service/SrtTranslationService.java`)
   - Service interface for SRT translation operations

4. **SrtTranslationServiceImpl.java** (`service/impl/SrtTranslationServiceImpl.java`)
   - Implementation of SRT translation service using DeepL API
   - Handles file reading, parsing, translation, and SRT generation

5. **SrtTranslationController.java** (`controller/SrtTranslationController.java`)
   - REST controller handling translation requests
   - Endpoints:
     - `POST /api/srt/translation/translateEnToEl` - Translate SRT file
     - `GET /api/srt/translation/download` - Download translated file
     - `GET /api/srt/translation/listTranslations` - List all translations
     - `GET /api/srt/translation/maxFileSize` - Get max file size

6. **srt-translation.ftl** (`templates/srt-translation.ftl`)
   - FreeMarker template for SRT translation UI
   - Features: Drag-and-drop upload, progress bar, file list

### Modified Files

1. **pom.xml**
   - Added DeepL Java API dependency

2. **application.properties**
   - Added DeepL API key configuration

3. **HomeController.java**
   - Added `/srt-translation-page` endpoint

4. **home.ftl**
   - Added link to SRT translation page

## SRT File Format

The service supports standard SRT subtitle format:

```
1
00:01:03,812 --> 00:01:07,248
<i>I remember you back in the GPO</i>

2
00:01:07,583 --> 00:01:11,178
<i>Laughin' with McDermott</i>
```

### Format Rules

- Index: Unique number (1, 2, 3, ...)
- Timecode: `HH:MM:SS,mmm --> HH:MM:SS,mmm`
- Text: Subtitle text (can span multiple lines)
- Blank line separates entries

## Configuration

### DeepL API Key

The service requires a DeepL API key. You can provide it in three ways:

1. **Environment Variable**: Set `DEEPL_API_KEY` environment variable
2. **Application Properties**: Set `deepl.api.key` in application.properties
3. **System Variable**: Spring will read from system properties

Example in `application-local.properties`:
```properties
deepl.api.key=your-deepl-api-key-here
```
**Note:** Or have configured `DEEPL_API_KEY` environment variable.

Get your API key from: https://www.deepl.com/pro-api

### File Size Limits

- Default max file size: 50 MB (configurable in `SrtTranslationServiceImpl.java`)
- Max total storage per container: 100 MB (shared with other features)

## API Endpoints

### 1. Translate SRT File

**Request:**
```
POST /api/srt/translation/translateEnToEl
Content-Type: multipart/form-data

file: <SRT file>
```

**Response:**
```
HTTP 200 OK
X-Filename: translated_en_el_movie_20260315_120530.srt

Translation completed successfully. File saved as: translated_en_el_movie_20260315_120530.srt
```

**Error Responses:**
- `400 Bad Request` - Invalid file format or size exceeds limit
- `500 Internal Server Error` - Translation failed

### 2. Download Translated File

**Request:**
```
GET /api/srt/translation/download?fileName=translated_en_el_movie_20260315_120530.srt
```

**Response:**
- Binary SRT file as attachment

### 3. List Translations

**Request:**
```
GET /api/srt/translation/listTranslations
```

**Response:**
```
Translated SRT files:
  - translated_en_el_movie1_20260315_120530.srt
  - translated_en_el_movie2_20260315_121015.srt
```

### 4. Get Max File Size

**Request:**
```
GET /api/srt/translation/maxFileSize
```

**Response:**
```
Maximum file size: 50 MB
```

## UI Flow

1. **Home Page**: Click "Go to SRT Translation" button
2. **SRT Translation Page**:
   - Drag-and-drop or click to select `.srt` file
   - Click "Translate to Greek" button
   - Monitor progress bar
   - See success/error message
   - View list of translated files
   - Download completed translations

## Translation Process

1. **File Upload**: Receive and validate SRT file
2. **Parse**: Extract subtitle entries with validation
3. **Optimize**: 
   - Remove HTML tags for sending to API
   - Prepare minimum text for translation
4. **Translate**: Send each subtitle to DeepL API
   - Source Language: English (EN)
   - Target Language: Greek (EL)
5. **Restore**: Add HTML tags back to translated text
6. **Generate**: Create new SRT file with translations
7. **Save**: Upload to shared container with timestamp

## Error Handling

The service handles various error scenarios:

- **Invalid File Format**: Detected via regex pattern matching
- **Empty File**: Checked before processing
- **Size Exceeded**: Validated against limits
- **Missing API Key**: Throws `IllegalStateException`
- **Translation Error**: Caught and reported per subtitle
- **File I/O Error**: Handled with proper logging

## Logging

All operations are logged at appropriate levels:
- `INFO`: Major operations (translation started/completed)
- `DEBUG`: Detailed processing (individual subtitles)
- `WARN`: Validation issues (invalid timecode)
- `ERROR`: Failures (translation failed, I/O errors)

## Example Usage

### Using curl
```bash
curl -X POST http://localhost:8080/api/srt/translation/translateEnToEl \
  -F "file=@movie_subtitles.srt"
```

### Using JavaScript
```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('/api/srt/translation/translateEnToEl', {
  method: 'POST',
  body: formData
})
.then(response => response.text())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

## Performance Considerations

1. **API Rate Limiting**: DeepL has rate limits - ensure your API plan supports expected volume
2. **Translation Time**: Depends on subtitle count and API response time
3. **File Size**: Larger files will take longer to process
4. **Storage**: Translated files stored in local VM storage

## Troubleshooting

### "DeepL API key not configured"
- Ensure `DEEPL_API_KEY` environment variable is set
- Or configure `deepl.api.key` in application properties

### "Invalid SRT file format"
- Verify file follows standard SRT format
- Check for proper timecode format
- Ensure blank lines between entries

### "File size exceeds maximum"
- Split large files into smaller chunks
- Or modify `MAX_FILE_SIZE_MB` in `SrtTranslationServiceImpl`

### Translation appears incomplete
- Check DeepL API quotas
- Verify API key has sufficient credits
- Check logs for specific error messages

## Future Enhancements

- Support for other language pairs
- Batch file processing
- Progress webhook notifications
- Translation caching
- Custom HTML tag handling
- Subtitle synchronization adjustment


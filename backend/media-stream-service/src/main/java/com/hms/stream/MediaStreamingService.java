package com.hms.stream;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hms.shared.media.MediaCategory;
import com.hms.shared.messaging.catalogupdates.CatalogUpdate;
import com.hms.shared.messaging.catalogupdates.CatalogUpdateType;
import com.hms.stream.messaging.CatalogUpdateProducer;

@Service
public class MediaStreamingService {

    private static final String SAMPLE_PLAYBACK_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4";
    private final Path moviesRoot = Paths.get("media", "movies");
    private final Path seriesRoot = Paths.get("media", "series");

    public MediaStreamingService() {
        try {
            Files.createDirectories(moviesRoot);
            Files.createDirectories(seriesRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload directory", e);
        }
    }

    public StreamManifestResponse manifest(String mediaId, String playbackUrl) {
        List<CaptionTrack> tracks = List.of(
                new CaptionTrack("en", "English", "/api/stream/" + mediaId + "/captions?lang=en"),
                new CaptionTrack("es", "Spanish", "/api/stream/" + mediaId + "/captions?lang=es"));

        // String resolvedPlaybackUrl = playbackUrl == null || playbackUrl.isBlank() ?
        // SAMPLE_PLAYBACK_URL : playbackUrl;

        MediaRecord record;

        try {
            record = new MediaRecord.Dao().get(mediaId);
        } catch (Exception e) {
            // Log the error and fall back to sample playback URL
            System.err.println("Failed to retrieve media record from database: " + e.getMessage());
            record = null;
        }

        String resolvedPlaybackUrl;
        if (record != null) {
            resolvedPlaybackUrl = "api/stream/files/" + mediaId;
            // record.filePath();
        } else {
            resolvedPlaybackUrl = SAMPLE_PLAYBACK_URL;
        }

        return new StreamManifestResponse(
                mediaId,
                resolvedPlaybackUrl,
                tracks);
    }

    public UploadMediaResponse upload(MultipartFile file, UploadMediaRequest body) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }

        String mediaId = UUID.randomUUID().toString();
        FileExtension ext = FileExtension.fromFilename(file.getOriginalFilename());
        if (ext == FileExtension.ZIP) {
            // support zips and extract the folder structure as needed
            throw new UnsupportedOperationException("ZIP file uploads are not yet supported");
        }

        String storedFilename = mediaId + ext.getExtension();
        MediaCategory type = MediaCategory.valueOf(body.type().toUpperCase(Locale.ROOT));
        Path destination;
        if (type == MediaCategory.MOVIE) {
            destination = moviesRoot.resolve(storedFilename).normalize();
        } else if (type == MediaCategory.SERIES) {
            destination = seriesRoot.resolve(storedFilename).normalize();
        } else {
            throw new IllegalArgumentException("Invalid media type: " + body.type());
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        MediaRecord record = new MediaRecord(mediaId, destination.toString());
        try {
            new MediaRecord.Dao().insert(record);

        } catch (Exception e) {
            destination.toFile().delete(); // Cleanup the stored file if database insert fails
            throw new IllegalStateException("Failed to save media record to database", e);
        }

        CatalogUpdate update = new CatalogUpdate(mediaId, CatalogUpdateType.CREATED, file.getOriginalFilename(), type,
                body.year(), body.description());
        CatalogUpdateProducer.postMessage(update);

        return new UploadMediaResponse(
                mediaId,
                "/api/stream/files/" + mediaId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize());
    }

    public ResponseEntity<Resource> file(String storageId) {
        Path filePath = findStoredFile(storageId);
        Resource resource = toResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = probeContentType(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);
    }

    public String vtt(String mediaId, String lang) {
        String safeLang = lang == null || lang.isBlank() ? "en" : lang;
        return "WEBVTT\n\n"
                + "00:00:01.000 --> 00:00:05.000\n"
                + "Media " + mediaId + " subtitles (" + safeLang + ") initialized.\n\n"
                + "00:00:06.000 --> 00:00:10.000\n"
                + "High-performance stream sample is now playing.\n";
    }

    private String extension(String filename) {
        if (filename == null || filename.isBlank()) {
            return ".mp4";
        }

        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return ".mp4";
        }

        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private Path findStoredFile(String storageId) {
        if (storageId == null || storageId.isBlank()) {
            throw new IllegalArgumentException("storageId is required");
        }

        MediaRecord record;
        try {
            record = new MediaRecord.Dao().get(storageId);
        } catch (Exception e) {
            // Log the error and fall back to searching the media directories
            System.err.println("Failed to retrieve media record from database: " + e.getMessage());
            record = null;
        }
        if (record == null) {
            throw new IllegalArgumentException("Media record not found for storageId: " + storageId);
        }
        Path exact = Path.of(record.filePath()).normalize();
        if (Files.exists(exact) && Files.isRegularFile(exact)) {
            return exact;
        } else {
            throw new IllegalArgumentException("Stored file not found for storageId: " + storageId);
        }
        // Path exact = moviesRoot.resolve(storageId).normalize();
        // if (Files.exists(exact) && Files.isRegularFile(exact)) {
        // return exact;
        // }

        // try (Stream<Path> files = Files.list(moviesRoot)) {
        // return files
        // .filter(path -> path.getFileName().toString().startsWith(storageId + "."))
        // .findFirst()
        // .orElseThrow(() -> new IllegalArgumentException("Uploaded file not found"));
        // } catch (IOException e) {
        // throw new IllegalStateException("Failed to locate uploaded file", e);
        // }

    }

    private Resource toResource(Path filePath) {
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }

    private String probeContentType(Path filePath) {
        try {
            String detected = Files.probeContentType(filePath);
            if (detected != null && !detected.isBlank()) {
                return detected;
            }
        } catch (IOException ignored) {
            // Fall through to default binary content type.
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}

enum FileExtension {
    MP4(".mp4"),
    MKV(".mkv"),
    AVI(".avi"),
    MOV(".mov"),
    ZIP(".zip");

    private final String extension;

    FileExtension(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static FileExtension fromFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return MP4; // Default to MP4 for unknown or missing filenames
        }

        String lower = filename.toLowerCase(Locale.ROOT);
        for (FileExtension ext : values()) {
            if (lower.endsWith(ext.extension)) {
                return ext;
            }
        }
        return MP4; // Default to MP4 if no known extension is found
    }
}
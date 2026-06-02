package com.hms.stream;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaStreamingService {

    public StreamManifestResponse manifest(String mediaId) {
        List<CaptionTrack> tracks = List.of(
                new CaptionTrack("en", "English", "/api/stream/" + mediaId + "/captions?lang=en"),
                new CaptionTrack("es", "Spanish", "/api/stream/" + mediaId + "/captions?lang=es")
        );

        return new StreamManifestResponse(
                mediaId,
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                tracks
        );
    }

    public String vtt(String mediaId, String lang) {
        String safeLang = lang == null || lang.isBlank() ? "en" : lang;
        return "WEBVTT\n\n"
                + "00:00:01.000 --> 00:00:05.000\n"
                + "Media " + mediaId + " subtitles (" + safeLang + ") initialized.\n\n"
                + "00:00:06.000 --> 00:00:10.000\n"
                + "High-performance stream sample is now playing.\n";
    }
}

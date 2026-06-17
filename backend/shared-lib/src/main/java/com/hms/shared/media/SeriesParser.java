package com.hms.shared.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.hms.shared.dao.SQLiteMap;

public class SeriesParser {

    private final Pattern seriesPattern = Pattern
            .compile("^(?<seriesName>.+)\\.S(?<seasonNumber>\\d+)E(?<episodeNumber>\\d+)\\.(?<episodeName>.+)\\..+$");

    private List<String> filePaths;

    protected SeriesParser(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public List<Series> parse() {
        // This must look at the file paths and determine what series, season and
        // episode each path belongs to
        // The.Office.US.S01E01.Pilot.720p.WEBRip.2CH.x265.HEVC-PSA

        Map<String, Series> seriesMap = new SQLiteMap<>(Series.class);

        for (String string : filePaths) {
            seriesPattern.matcher(string).results().forEach(result -> {
                String seriesName = result.group("seriesName");
                int seasonNumber = Integer.parseInt(result.group("seasonNumber"));
                int episodeNumber = Integer.parseInt(result.group("episodeNumber"));
                String episodeName = result.group("episodeName").replaceAll("\\.[0-9]{3,4}p.*$", "");

                Series series = seriesMap.get(seriesName);
                if (series == null) {
                    series = new Series(UUID.randomUUID().toString(), seriesName, new ArrayList<>());
                    seriesMap.put(seriesName, series);
                }

                Season season = series.seasons().stream()
                        .filter(s -> s.seasonNumber() == seasonNumber)
                        .findFirst()
                        .orElse(null);

                if (season == null) {
                    season = new Season(UUID.randomUUID().toString(), series.seriesId(),
                            seriesName + " S" + seasonNumber, seasonNumber, new ArrayList<>());
                    series.seasons().add(season);
                }

                season.episodes().add(new Episode(UUID.randomUUID().toString(), season.seasonId(), series.seriesId(),
                        episodeName, episodeNumber, string));
            });
        }

        return seriesMap.values().stream().toList();
    }

    public static Builder builder() {
        return new Builder();
    }

}

class Builder {
    private List<String> filePaths = new ArrayList<>();

    public Builder addFilePath(String filePath) {
        filePaths.add(filePath);
        return this;
    }

    public Builder addFilePaths(List<String> filePaths) {
        this.filePaths.addAll(filePaths);
        return this;
    }

    public SeriesParser build() {
        return new SeriesParser(filePaths);
    }
}
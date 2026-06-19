package com.hms.shared.sql.media;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.hms.shared.dao.SQLiteMap;

// public class SeriesParser {

//     private final Pattern seriesPattern = Pattern
//             .compile("^(?<seriesName>.+)\\.S(?<seasonNumber>\\d+)E(?<episodeNumber>\\d+)\\.(?<episodeName>.+)\\..+$");

//     private List<String> filePaths;

//     protected SeriesParser(List<String> filePaths) {
//         this.filePaths = filePaths;
//     }

//     public List<Series> parse() {
//         // This must look at the file paths and determine what series, season and
//         // episode each path belongs to
//         // The.Office.US.S01E01.Pilot.720p.WEBRip.2CH.x265.HEVC-PSA

//         // SQLiteMap<Series> seriesMap = new SQLiteMap<>(new Series.Dao());
//         // SQLiteMap<Season> seasonMap = new SQLiteMap<>(new Season.Dao());
//         // SQLiteMap<Episode> episodeMap = new SQLiteMap<>(new Episode.Dao());

//         Map<String, Series> seriesMap = new HashMap<>();

//         for (String string : filePaths) {
//             seriesPattern.matcher(string).results().forEach(result -> {
//                 String seriesName = result.group("seriesName");
//                 int seasonNumber = Integer.parseInt(result.group("seasonNumber"));
//                 int episodeNumber = Integer.parseInt(result.group("episodeNumber"));
//                 String episodeName = result.group("episodeName").replaceAll("\\.?[0-9]{3,4}p.*$", "");

//                 if (episodeName.isEmpty()) {
//                     episodeName = "Episode " + episodeNumber;
//                 }

//                 Series series = seriesMap.values().stream()
//                         .filter(s -> s.name().equals(seriesName))
//                         .findFirst()
//                         .orElse(null);

//                 if (series == null) {
//                     series = new Series(UUID.randomUUID().toString(), seriesName, new ArrayList<>());
//                     seriesMap.put(series.seriesId(), series);
//                 }

//                 Season season = series.seasons().stream()
//                         .filter(s -> s.seasonNumber() == seasonNumber)
//                         .findFirst()
//                         .orElse(null);

//                 if (season == null) {
//                     season = new Season(UUID.randomUUID().toString(), series.seriesId(),
//                             seriesName + " S" + seasonNumber, seasonNumber, new ArrayList<>());

//                     series = series.addSeason(season);
//                     seriesMap.put(series.seriesId(), series);
//                 }

//                 MediaItem mediaItem = new MediaItem(UUID.randomUUID().toString(), string);

//                 Episode episode = new Episode(UUID.randomUUID().toString(), season.seasonId(), series.seriesId(),
//                         mediaItem, episodeName, episodeNumber);
//                 season = season.addEpisode(episode);
//                 series = series.addSeason(season);
//                 seriesMap.put(series.seriesId(), series);
//             });
//         }

//         return seriesMap.values().stream().toList();
//     }

//     public static Builder builder() {
//         return new Builder();
//     }

//     public static class Builder {
//         private List<String> filePaths = new ArrayList<>();

//         public Builder addFilePath(String filePath) {
//             filePaths.add(filePath);
//             return this;
//         }

//         public Builder addFilePaths(List<String> filePaths) {
//             this.filePaths.addAll(filePaths);
//             return this;
//         }

//         public SeriesParser build() {
//             return new SeriesParser(filePaths);
//         }
//     }

// }

package com.hms.annotated.sql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.hms.annotated.sql.media.Episode;
import com.hms.annotated.sql.media.MediaInfo;
import com.hms.annotated.sql.media.MediaItem;
import com.hms.annotated.sql.media.Season;
import com.hms.annotated.sql.media.Series;
import com.hms.annotated.sql.view.ViewRecordDao;

@TableRecord(dbName = ExampleRecord.TABLE_NAME)
public record ExampleRecord(@PrimaryKey String id, String name) {
    static final String TABLE_NAME = "example";

    public static void main(String[] args) throws Exception {
        // System.out.println("Hello, World!");
        // SqlRecordFactory<ExampleRecord> factory =
        // SqlRecordFactory.getFactory(ExampleRecord.class);
        // var record = new ExampleRecord("1", "Test");
        // factory.insert(record);

        // var fetchRecord = factory.select(Map.of("id", "1"));

        // System.out.println("Fetched record: " + fetchRecord);

        // record = new ExampleRecord("1", "Updated Test");
        // factory.update(record);

        // fetchRecord = factory.select(Map.of("id", "1"));
        // System.out.println("Fetched record after update: " + fetchRecord);

        // factory.delete(record);w
        // try {
        //     Series series = new Series("series1", "My Series", List.of(
        //             new Season("season1", "series1", "Season 1", 1, List.of(
        //                     new Episode("episode1", "season1", "series1",
        //                             "Episode 1", 1, new MediaItem("media1", "/path/to/media1")),
        //                     new Episode("episode2", "season1", "series1",
        //                             "Episode 2", 2, new MediaItem("media2", "/path/to/media2")))),
        //             new Season("season2", "series1", "Season 2", 2, List.of(
        //                     new Episode("episode3", "season2", "series1",
        //                             "Episode 3", 1, new MediaItem("media3", "/path/to/media3"))))));

        //     SqlRecordDao<Series> seriesFactory = SqlRecordDao.getFactory(Series.class);
        //     seriesFactory.insert(series);

        //     var fetchedSeries = seriesFactory.select(Map.of("seriesId", "series1"));
        //     System.out.println("Fetched series: " + fetchedSeries);

        //     var mediaInfoFactory = ViewRecordDao.getFactory(MediaInfo.class);
        //     var fetchedMediaInfo = mediaInfoFactory.select(Map.of("mediaId", "media1"));
        //     System.out.println("Fetched media info: " + fetchedMediaInfo);

        //     seriesFactory.delete(series);
        //     var fetchedSeriesAfterDelete = seriesFactory.select(Map.of("seriesId", "series1"));
        //     System.out.println("Fetched series after delete: " + fetchedSeriesAfterDelete);
        // } finally {
        //     // // Clean up the database after testing
        //     Files.deleteIfExists(Path.of("database", ExampleRecord.TABLE_NAME));
        //     Files.deleteIfExists(Path.of("database", Series.MEDIA_TABLE));
        // }
    }
}

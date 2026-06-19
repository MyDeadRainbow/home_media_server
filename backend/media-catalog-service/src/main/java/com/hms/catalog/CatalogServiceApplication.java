package com.hms.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatalogServiceApplication {
    CatalogServiceApplication() {
    }

    public static void main(String[] args) {
        // SeriesParser seriesParser = SeriesParser.builder()
        // .addFilePath("The.Office.US.S01E01.Pilot.720p.WEBRip.2CH.x265.HEVC-PSA").build();
        // seriesParser.parse().forEach((series) -> {
        // System.out.println("Series: " + series);
        // try {
        // new Series.Dao().insert(series);
        // } catch (SQLException e) {
        // e.printStackTrace();
        // }
        // });

        // try {
        // new Movie.Dao().ensureTableExists();
        // System.out.println(new MediaInfo.Dao().search("Office"));
        // } catch (Exception e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}

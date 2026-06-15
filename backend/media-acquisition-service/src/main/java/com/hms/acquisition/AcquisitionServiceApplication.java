package com.hms.acquisition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hms.acquisition.importmedia.ImportMediaEntry;
import com.hms.acquisition.importmedia.ImportMediaStatus;
import com.hms.acquisition.importmedia.magnetfinder.LimeTorrentMagnetFinder;
import com.hms.acquisition.importmedia.pipeline.ImportMediaPipeline;

@SpringBootApplication
public class AcquisitionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcquisitionServiceApplication.class, args);
    }
}

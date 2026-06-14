package com.hms.acquisition;

import org.libtorrent4j.SessionManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hms.acquisition.importmedia.TorrentMagnetLink;

@SpringBootApplication
public class AcquisitionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcquisitionServiceApplication.class, args);
    }
}

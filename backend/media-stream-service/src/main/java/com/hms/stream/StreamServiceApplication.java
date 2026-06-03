package com.hms.stream;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StreamServiceApplication {

    public static void main(String[] args) {
        File mediaDir = new File("media");
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        SpringApplication.run(StreamServiceApplication.class, args);
    }
}

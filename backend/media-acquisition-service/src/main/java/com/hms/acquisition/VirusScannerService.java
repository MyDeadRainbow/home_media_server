package com.hms.acquisition;

import org.springframework.stereotype.Service;

@Service
public class VirusScannerService {

    public boolean scanFolder(String folderPath) {
        String normalized = folderPath.toLowerCase();
        return !(normalized.endsWith(".exe") || normalized.contains("ransom"));
    }
}

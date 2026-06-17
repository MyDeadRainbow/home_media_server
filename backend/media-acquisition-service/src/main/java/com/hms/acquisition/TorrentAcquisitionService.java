package com.hms.acquisition;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.stereotype.Service;

// import com.hms.acquisition.importmedia.ImportMediaEntry;
// import com.hms.acquisition.importmedia.ImportMediaRequest;
// import com.hms.acquisition.importmedia.ImportMediaStatus;

// @Service
// public class TorrentAcquisitionService {

//     private static final Logger LOG = LoggerFactory.getLogger(TorrentAcquisitionService.class);

//     private final VirusScannerService virusScannerService;

//     public TorrentAcquisitionService(VirusScannerService virusScannerService) {
//         this.virusScannerService = virusScannerService;
//     }

//     public boolean addImportRequest(ImportMediaRequest request) {
//         ImportMediaEntry entry = new ImportMediaEntry(
//                 java.util.UUID.randomUUID().toString(),
//                 request.title(),
//                 ImportMediaStatus.PENDING,
//                 null
//             );

//         try {
//             entry.insert();
//         } catch (Exception e) {
//             LOG.error("Failed to add media request", e);
//             return false;
//         }

//         return true;
//     }
// }

package com.hms.stream.importmedia;

import java.io.File;
import java.nio.file.Files;

import org.libtorrent4j.AddTorrentParams;
import org.libtorrent4j.BDecodeNode;
import org.libtorrent4j.ErrorCode;
import org.libtorrent4j.SessionManager;
import org.libtorrent4j.TorrentHandle;
import org.libtorrent4j.TorrentInfo;

import com.hms.stream.importmedia.pipeline.ImportMediaHandler;

// public class ResumeTorrentHandler implements ImportMediaHandler {

//     @Override
//     public ImportMediaEntry handle(ImportMediaEntry entry) throws Exception {
//         if (entry.resumeFile() == null || entry.resumeFile().isEmpty()) {
//             throw new IllegalArgumentException("Resume file is missing for entry: " + entry.id());
//         }

//         // 1. Read the .fastresume file to a byte array
//         File resumeFile = new File(entry.resumeFile());
//         byte[] resumeData = Files.readAllBytes(resumeFile.toPath());

//         // 2. Bdecode the data
//         BDecodeNode bdecodeNode = BDecodeNode.bdecode(resumeData);

//         try (TorrentSession torrentSession = TorrentSession.getInstance()) {
//             SessionManager session = torrentSession.getSessionManager();
//             // 3. Create params for your torrent and apply the resume data
//             TorrentInfo torrentInfo = new TorrentInfo(resumeData
//                 // session.fetchMagnet(entry.magnetLink(), 30,
//                 //     TorrentMagnetLink.TEMP_FOLDER.resolve(entry.id()).toFile())
//                 );            
            
//             AddTorrentParams params = new AddTorrentParams();
//             params.setTorrentInfo(torrentInfo);
//             params.setSavePath("path/to/download/directory"); // Set your desired download path here

//             // ErrorCode error = new ErrorCode();
//             // params.(bdecodeNode, error);
                
//             // Ensure there were no errors before adding to the session
//             if (error.value() == 0) {
//                 TorrentHandle handle = session.addTorrent(params);
//             } else {
//                 System.err.println("Failed to read resume data: " + error.message());
//             }
//         }

//     }

// }

package com.hms.stream.torrentinfo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream/torrent")
public class TorrentInfoController {
    private final TorrentInfoService torrentInfoService;

    public TorrentInfoController(TorrentInfoService torrentInfoService) {
        this.torrentInfoService = torrentInfoService;
    }

    @GetMapping("/info")
    public ResponseEntity<List<TorrentInfoResponse>> getTorrentInfo() {
        return new ResponseEntity<>(torrentInfoService.getTorrentInfo(), HttpStatus.OK);
    }

    @PostMapping("/pause/{infoHash}")
    public ResponseEntity<Boolean> postPause(@PathVariable String infoHash) {
        return new ResponseEntity<>(torrentInfoService.pauseTorrent(infoHash), HttpStatus.OK);
    }

    @PostMapping("/resume/{infoHash}")
    public ResponseEntity<Boolean> postResume(@PathVariable String infoHash) {
        return new ResponseEntity<>(torrentInfoService.resumeTorrent(infoHash), HttpStatus.OK);
    }

    @PostMapping("/delete/{infoHash}")
    public ResponseEntity<Boolean> postDelete(@PathVariable String infoHash) {
        torrentInfoService.cancelEmitter(infoHash);
        torrentInfoService.cancelInfoStream(infoHash);
        return new ResponseEntity<>(torrentInfoService.deleteTorrent(infoHash), HttpStatus.OK);
    }

    @PostMapping("/reorder/{infoHash}/{newPosition}")
    public ResponseEntity<Boolean> postReorder(@PathVariable String infoHash, @PathVariable int newPosition) {
        // TODO: process POST request

        return new ResponseEntity<>(false, HttpStatus.NOT_IMPLEMENTED);
    }

    @GetMapping("/infostream")
    public ResponseEntity<SseEmitter> getInfoStream(@RequestParam String infoHash) {
        ResponseEntity<SseEmitter> response = torrentInfoService.getTorrentInfoStream(infoHash);
        return response;
    }

    @GetMapping("/media/infostream")
    public ResponseEntity<SseEmitter> getMediaItemInfoStream(@RequestParam String mediaItemId) {
        ResponseEntity<SseEmitter> response = torrentInfoService.getMediaItemInfoStream(mediaItemId);
        return response;
    }
    

}

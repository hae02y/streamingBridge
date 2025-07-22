package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/rtsp/v1")
@Slf4j
public class RtspController {


    private final Map<String, String> cameraRegistry = Map.of(
            "cam1", "rtsp://210.99.70.120:1935/live/cctv001.stream",
            "cam2", "rtsp://210.99.70.120:1935/live/cctv002.stream",
            "cam3", "rtsp://210.99.70.120:1935/live/cctv003.stream"
    );

    private final Map<String, Process> cameraProcesses = new ConcurrentHashMap<>();

    /**
     * 카메라 목록 반환
     */
    @GetMapping("/cameras")
    public ResponseEntity<Map<String, String>> listCameras() {
        return ResponseEntity.ok(cameraRegistry);
    }

    /**
     * 카메라 시작
     */
    @PostMapping("/start")
    public ResponseEntity<String> startStream(@RequestParam("cameraId") String cameraId) {

        if (!cameraRegistry.containsKey(cameraId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unknown camera: " + cameraId);
        }

        if (cameraProcesses.containsKey(cameraId) && cameraProcesses.get(cameraId).isAlive()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Camera already running: " + cameraId);
        }

        File hlsDir = new File("/tmp/hls/" + cameraId);
        if (!hlsDir.exists()) {
            hlsDir.mkdirs();
        }

        String rtspUrl = cameraRegistry.get(cameraId);
        String outputPath = String.format("/tmp/hls/%s/stream.m3u8", cameraId);

        String[] command = {
                "ffmpeg",
                "-rtsp_transport", "tcp",
                "-i", rtspUrl,
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-tune", "zerolatency",
                "-f", "hls",
                "-hls_time", "1",
                "-hls_list_size", "3",
                "-hls_flags", "delete_segments",
                outputPath
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.inheritIO();
            Process process = pb.start();
            cameraProcesses.put(cameraId, process);
            log.info("Started camera [{}] at [{}]", cameraId, rtspUrl);
            return ResponseEntity.ok("Stream started for camera: " + cameraId);
        } catch (IOException e) {
            log.error("Failed to start camera: {}", cameraId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start stream for camera: " + cameraId);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopStream(@RequestParam("cameraId") String cameraId) {
        Process process = cameraProcesses.get(cameraId);
        if (process != null && process.isAlive()) {
            process.destroy();
            cameraProcesses.remove(cameraId);
            log.info("Stopped camera [{}]", cameraId);
            return ResponseEntity.ok("Stream stopped for camera: " + cameraId);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No running stream for camera: " + cameraId);
        }
    }
}

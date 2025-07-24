package com.example.demo.controller;

import com.example.demo.registry.CameraRegistry;
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

    private final CameraRegistry cameraRegistry;

    public RtspController(CameraRegistry cameraRegistry) {
        this.cameraRegistry = cameraRegistry;
    }

    private final Map<String, Process> cameraProcesses = new ConcurrentHashMap<>();


    @GetMapping("/cameras")
    public ResponseEntity<Map<String, String>> listCameras() {
        return ResponseEntity.ok(cameraRegistry.getAllCameras());
    }

    @PostMapping("/start")
    public ResponseEntity<String> startStream(@RequestParam("cameraId") String cameraId) {

        if (!cameraRegistry.contains(cameraId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("해당하는 카메라가 없습니다. " + cameraId);
        }

        if (cameraProcesses.containsKey(cameraId) && cameraProcesses.get(cameraId).isAlive()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("해당 카메라 스트리밍 진행중입니다. " + cameraId);
        }

        File hlsDir = new File("/tmp/hls/" + cameraId);
        if (!hlsDir.exists()) {
            hlsDir.mkdirs();
        }

        String rtspUrl = cameraRegistry.getCameraUrl(cameraId);
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
            return ResponseEntity.ok("카메라 스트리밍이 시작되었습니다. " + cameraId);
        } catch (IOException e) {
            log.error("카메라 스트리밍이 실패하였습니다. {}", cameraId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("카메라 스트리밍이 실패하였습니다. " + cameraId);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopStream(@RequestParam("cameraId") String cameraId) {
        Process process = cameraProcesses.get(cameraId);
        if (process != null && process.isAlive()) {
            process.destroy();
            cameraProcesses.remove(cameraId);
            log.info("Stopped camera [{}]", cameraId);
            return ResponseEntity.ok("카메라 스트리밍이 정상적으로 중지되었습니다. " + cameraId);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("카메라 스트리밍이 진행중이지 않습니다. " + cameraId);
        }
    }
}

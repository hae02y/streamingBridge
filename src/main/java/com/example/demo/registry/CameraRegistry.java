package com.example.demo.registry;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class CameraRegistry {
    private final Map<String, String> cameraMap = Map.of(
            "cam1", "rtsp://210.99.70.120:1935/live/cctv001.stream",
            "cam2", "rtsp://210.99.70.120:1935/live/cctv002.stream",
            "cam3", "rtsp://210.99.70.120:1935/live/cctv003.stream"
    );

    public Map<String, String> getAllCameras() {
        return cameraMap;
    }

    public String getCameraUrl(String cameraId) {
        return cameraMap.get(cameraId);
    }

    public boolean contains(String cameraId) {
        return cameraMap.containsKey(cameraId);
    }
}

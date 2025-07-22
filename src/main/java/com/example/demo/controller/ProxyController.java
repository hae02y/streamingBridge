package com.example.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProxyController {

    private final WebClient webClient;

    @GetMapping("/olive/master.m3u8")
    public Mono<ResponseEntity<String>> getM3u8Olive(@RequestParam("entry") String entry) {
        return webClient.get()
                .uri("https://la-poc-cctv-2-api.us.ddns.vanager.ai/hls/7a0dabbf-753b-73d5-9a81-44048fddcd27.m3u?pos=" + entry)
                .header("x-runtime-guid", "vms-f35cf93d4d1ccc76d561e2e0caef76bd-RIfyOwMqgM")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl");
                    return new ResponseEntity<>(response, headers,HttpStatus.OK);
                });
    }

    @GetMapping("/hill/master.m3u8")
    public Mono<ResponseEntity<String>> getM3u8Hill(@RequestParam("entry") String entry) {
        return webClient.get()
                .uri("https://la-poc-cctv-1-api.us.ddns.vanager.ai/hls/b42b485f-bc5c-b4f6-c991-4187f41b66fc.m3u?pos=" + entry)
                .header("x-runtime-guid", "vms-3250113c259ee50e35d53e24cda8a06d-s0crphR5aX")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl");
                    return new ResponseEntity<>(response, headers,HttpStatus.OK);
                });
    }
}
package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SSEController {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @CrossOrigin(origins = "*")
    @PostMapping("/stream")
    public SseEmitter streamData(@RequestBody String data) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        executorService.submit(() -> {
            try {
                for (int i = 0; i < 30; i++) {
                    emitter.send(SseEmitter.event().name("message").data(data + " - " + i));
                    Thread.sleep(1000);
                }
                emitter.send(SseEmitter.event().name("end").data("Stream ended"));
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
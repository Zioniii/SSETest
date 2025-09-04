package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SSEController {
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @CrossOrigin(origins = "*")
    @GetMapping("/stream")
    public SseEmitter streamConnection() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String id = String.valueOf(System.currentTimeMillis());
        emitters.put(id, emitter);
        
        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        
        try {
            emitter.send(SseEmitter.event().name("connected").data("Connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/stream")
    public String streamData(@RequestBody String data) {
        // Send data to all connected clients
        emitters.values().forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("message").data(data));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        });
        
        // Start sending periodic messages
        executorService.submit(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    String message = data + " - " + i;
                    for (SseEmitter emitter : emitters.values()) {
                        emitter.send(SseEmitter.event().name("message").data(message));
                    }
                    Thread.sleep(1000);
                }
                
                // Send end event to all clients
                for (SseEmitter emitter : emitters.values()) {
                    emitter.send(SseEmitter.event().name("end").data("Stream ended"));
                    emitter.complete();
                }
            } catch (IOException | InterruptedException e) {
                for (SseEmitter emitter : emitters.values()) {
                    emitter.completeWithError(e);
                }
            }
        });
        
        return "Message sent";
    }
}
package com.example.seckill.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/traffic")
public class TrafficTestController {

    @GetMapping("/unstable")
    public ResponseEntity<Map<String, Object>> unstable(
            @RequestParam(defaultValue = "ok") String mode,
            @RequestParam(defaultValue = "0") int delayMs,
            @RequestParam(defaultValue = "0") int failPercent) throws InterruptedException {

        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }

        boolean failByRate = failPercent > 0 && ThreadLocalRandom.current().nextInt(100) < failPercent;
        boolean shouldFail = "fail".equalsIgnoreCase(mode) || failByRate;

        if (shouldFail) {
            throw new IllegalStateException("simulated upstream failure");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("mode", mode);
        body.put("delayMs", delayMs);
        body.put("failPercent", failPercent);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}

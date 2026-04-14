package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GatewayFallbackController {

    @RequestMapping("/fallback/seckill")
    public ResponseEntity<Map<String, Object>> seckillFallback() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("degraded", true);
        body.put("message", "service degraded by gateway governance");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}

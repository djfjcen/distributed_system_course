package com.example.seckill.controller;

import com.example.seckill.service.ReadWriteSplitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rw")
public class ReadWriteSplitController {

    private final ReadWriteSplitService readWriteSplitService;

    public ReadWriteSplitController(ReadWriteSplitService readWriteSplitService) {
        this.readWriteSplitService = readWriteSplitService;
    }

    @GetMapping("/write-host")
    public Map<String, Object> writeHost() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("route", "WRITE");
        body.put("db", readWriteSplitService.getWriteDataSourceInfo());
        return body;
    }

    @GetMapping("/read-host")
    public Map<String, Object> readHost() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("route", "READ");
        body.put("db", readWriteSplitService.getReadDataSourceInfo());
        return body;
    }
}

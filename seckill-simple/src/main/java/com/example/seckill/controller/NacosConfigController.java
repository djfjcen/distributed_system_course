package com.example.seckill.controller;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RefreshScope
public class NacosConfigController {

    private static final String DATA_ID = "seckill-simple.yaml";
    private static final String GROUP = "DEFAULT_GROUP";

    private final NacosConfigManager nacosConfigManager;

    @Value("${seckill.dynamic.message:hello-from-local-default}")
    private String dynamicMessage;

    @Value("${spring.application.name:unknown-app}")
    private String appName;

    public NacosConfigController(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

    @GetMapping("/message")
    public Map<String, Object> message() {
        String latestMessage = dynamicMessage;
        try {
            String latestYaml = nacosConfigManager.getConfigService().getConfig(DATA_ID, GROUP, 1000);
            if (latestYaml != null && !latestYaml.isBlank()) {
                Object loaded = new Yaml().load(latestYaml);
                if (loaded instanceof Map<?, ?> root) {
                    Object seckill = root.get("seckill");
                    if (seckill instanceof Map<?, ?> seckillMap) {
                        Object dynamic = seckillMap.get("dynamic");
                        if (dynamic instanceof Map<?, ?> dynamicMap) {
                            Object message = dynamicMap.get("message");
                            if (message instanceof String msg && !msg.isBlank()) {
                                latestMessage = msg;
                            }
                        }
                    }
                }
            }
        } catch (NacosException ignored) {
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appName", appName);
        body.put("dynamicMessage", latestMessage);
        return body;
    }
}

package com.example.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {
    // Spring Boot auto-configuration will handle Kafka setup based on application.yml
    // Producer and Consumer will be auto-configured from properties
}

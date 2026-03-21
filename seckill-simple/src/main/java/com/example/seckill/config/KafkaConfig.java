package com.example.seckill.config;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {
    // Spring Boot auto-configuration will handle Kafka setup based on application.yml
    // Producer and Consumer will be auto-configured from properties
}

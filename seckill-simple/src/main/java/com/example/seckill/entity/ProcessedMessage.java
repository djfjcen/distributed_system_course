package com.example.seckill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_message", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consumer_message", columnNames = {"consumer", "message_id"})
})
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String consumer;

    @Column(name = "message_id", nullable = false, length = 128)
    private String messageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}

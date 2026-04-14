package com.example.seckill.repository;

import com.example.seckill.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {

    boolean existsByConsumerAndMessageId(String consumer, String messageId);
}

package com.example.seckill.service;

import com.example.seckill.entity.ProcessedMessage;
import com.example.seckill.repository.ProcessedMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessedMessageService {

    private final ProcessedMessageRepository processedMessageRepository;

    public ProcessedMessageService(ProcessedMessageRepository processedMessageRepository) {
        this.processedMessageRepository = processedMessageRepository;
    }

    @Transactional(readOnly = true)
    public boolean alreadyProcessed(String consumer, String messageId) {
        return processedMessageRepository.existsByConsumerAndMessageId(consumer, messageId);
    }

    @Transactional
    public void markProcessed(String consumer, String messageId) {
        if (processedMessageRepository.existsByConsumerAndMessageId(consumer, messageId)) {
            return;
        }
        ProcessedMessage processedMessage = new ProcessedMessage();
        processedMessage.setConsumer(consumer);
        processedMessage.setMessageId(messageId);
        processedMessageRepository.save(processedMessage);
    }
}

package com.labourconnect.service;

import com.labourconnect.dto.WhatsAppBotJobEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "whatsapp-bot-job-requests";

    public void sendJobEvent(WhatsAppBotJobEvent event) {
        log.info("Attempting to send job event to Kafka topic '{}'. Provider: {}", TOPIC, event.providerId());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(TOPIC, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Kafka Message Sent Successfully! Topic: {}, Partition: {}, Offset: {}", 
                        TOPIC, 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send Kafka message to topic '{}'. Error: {}", TOPIC, ex.getMessage(), ex);
            }
        });
    }
}
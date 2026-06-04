package com.docplatform.ingest.event;

import com.docplatform.contract.ContractJson;
import com.docplatform.contract.DocumentLifecycleEvent;
import com.docplatform.contract.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DocumentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public DocumentEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DocumentLifecycleEvent event) {
        String key = event.docId().toString();
        String payload = ContractJson.write(event);
        kafkaTemplate.send(KafkaTopics.DOC_LIFECYCLE, key, payload);
    }
}

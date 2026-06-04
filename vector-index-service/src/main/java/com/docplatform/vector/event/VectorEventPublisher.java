package com.docplatform.vector.event;

import com.docplatform.contract.ContractJson;
import com.docplatform.contract.DocumentLifecycleEvent;
import com.docplatform.contract.KafkaTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class VectorEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public VectorEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DocumentLifecycleEvent event) {
        kafkaTemplate.send(KafkaTopics.DOC_LIFECYCLE, event.docId().toString(), ContractJson.write(event));
    }
}

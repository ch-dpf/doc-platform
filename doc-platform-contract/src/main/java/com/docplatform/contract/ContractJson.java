package com.docplatform.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ContractJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String write(DocumentLifecycleEvent event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }

    public static DocumentLifecycleEvent read(String json) {
        try {
            return MAPPER.readValue(json, DocumentLifecycleEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize event", e);
        }
    }

    private ContractJson() {
    }
}

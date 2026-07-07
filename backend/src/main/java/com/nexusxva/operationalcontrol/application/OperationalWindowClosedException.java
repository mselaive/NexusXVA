package com.nexusxva.operationalcontrol.application;

import java.util.Map;

public class OperationalWindowClosedException extends RuntimeException {

    private final Map<String, Object> metadata;

    public OperationalWindowClosedException(Map<String, Object> metadata) {
        super("Operational window is closed");
        this.metadata = Map.copyOf(metadata);
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}

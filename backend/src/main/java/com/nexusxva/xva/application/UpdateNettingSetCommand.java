package com.nexusxva.xva.application;

public record UpdateNettingSetCommand(
        String name,
        boolean active
) {
    public UpdateNettingSetCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("netting set name is required");
        }
        name = name.trim();
    }
}

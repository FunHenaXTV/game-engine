package com.tournament.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class IdShortener {

    private static final int PREFIX_LEN = 8;
    private final Map<String, UUID> prefixToFull = new HashMap<>();

    public String shorten(UUID id) {
        Objects.requireNonNull(id, "id");
        String prefix = id.toString().substring(0, PREFIX_LEN);
        prefixToFull.putIfAbsent(prefix, id);
        return prefix;
    }

    public Optional<UUID> resolve(String shortOrFull) {
        if (shortOrFull == null || shortOrFull.isBlank()) {
            return Optional.empty();
        }
        if (shortOrFull.length() >= 32) {
            try {
                return Optional.of(UUID.fromString(shortOrFull));
            } catch (IllegalArgumentException ignore) {
                return Optional.empty();
            }
        }
        UUID full = prefixToFull.get(shortOrFull);
        if (full != null) {
            return Optional.of(full);
        }
        for (Map.Entry<String, UUID> entry : prefixToFull.entrySet()) {
            if (entry.getKey().startsWith(shortOrFull)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}

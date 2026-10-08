package org.example.hexorise.client;
import tools.jackson.databind.*;
import tools.jackson.databind.node.ObjectNode;
import java.util.UUID;
public final class HighriseProtocol {
    private HighriseProtocol() {}
    public static ObjectNode chat(ObjectMapper mapper, String message, String whisperTarget) {
        if (message == null || message.isBlank() || message.length() > 255) throw new IllegalArgumentException("Chat must contain 1-255 characters");
        var request = mapper.createObjectNode().put("_type", "ChatRequest").put("message", message).put("rid", UUID.randomUUID().toString());
        if (whisperTarget != null) request.put("whisper_target_id", whisperTarget);
        return request;
    }
    public static long intervalMillis(JsonNode limits) {
        // Apply the strictest advertised bucket, without bursting; never faster than our 1s safety floor.
        long interval = 1000;
        for (JsonNode bucket : limits) {
            if (!bucket.isArray() || bucket.size() != 2 || bucket.get(0).asDouble() <= 0 || bucket.get(1).asDouble() <= 0)
                throw new IllegalArgumentException("Invalid session rate limit");
            interval = Math.max(interval, (long) Math.ceil(1000 * bucket.get(1).asDouble() / bucket.get(0).asDouble()));
        }
        return interval;
    }
}

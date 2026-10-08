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
    public static ObjectNode emote(ObjectMapper mapper, String emoteId, String target) {
        if (emoteId == null || !emoteId.matches("[a-z0-9-]{1,100}")) throw new IllegalArgumentException("Invalid emote ID");
        var request = mapper.createObjectNode().put("_type", "EmoteRequest").put("emote_id", emoteId).put("rid", UUID.randomUUID().toString());
        if (target != null) { requireUserId(target); request.put("target_user_id", target); }
        return request;
    }
    public static ObjectNode moderate(ObjectMapper mapper, String userId, String action, Integer seconds) {
        requireUserId(userId);
        if (!java.util.Set.of("kick", "ban", "unban", "mute").contains(action)) throw new IllegalArgumentException("Unsupported moderation action");
        if (seconds != null && (seconds < 1 || seconds > 86400 || !java.util.Set.of("mute", "ban").contains(action)))
            throw new IllegalArgumentException("Invalid moderation duration");
        var request = mapper.createObjectNode().put("_type", "ModerateRoomRequest").put("user_id", userId).put("moderation_action", action).put("rid", UUID.randomUUID().toString());
        if (seconds != null) request.put("action_length", seconds);
        return request;
    }
    private static void requireUserId(String userId) {
        if (userId == null || !userId.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalArgumentException("Invalid user ID");
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

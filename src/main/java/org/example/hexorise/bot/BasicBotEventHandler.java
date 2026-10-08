package org.example.hexorise.bot;
import tools.jackson.databind.JsonNode;
import org.example.hexorise.client.*;
import org.example.hexorise.config.HighriseProperties;
import org.example.hexorise.room.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
@Component
public class BasicBotEventHandler implements BotEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BasicBotEventHandler.class);
    private final RoomSettingsRepository settings;
    private final HighriseProperties properties;
    private final Map<String, Long> cooldowns = new HashMap<>();
    public BasicBotEventHandler(RoomSettingsRepository settings, HighriseProperties properties) {
        this.settings = settings; this.properties = properties;
    }
    @Override public void handle(JsonNode event, String botUserId, HighriseClient client) {
        String type = event.path("_type").asText();
        String userId = event.path("user").path("id").asText();
        if (userId.isBlank() || userId.equals(botUserId)) return;
        if ("UserLeftEvent".equals(type)) { cooldowns.remove(userId); return; }
        if (!"ChatEvent".equals(type) && !"UserJoinedEvent".equals(type)) return;
        RoomSettings room = settings.find(properties.roomId());
        String response = null;
        String target = null;
        if ("UserJoinedEvent".equals(type) && room.welcomeEnabled()) {
            response = room.welcomeMessage().replace("{username}", event.path("user").path("username").asText());
        } else if ("ChatEvent".equals(type)) {
            String message = event.path("message").asText().strip();
            if (!message.startsWith(room.commandPrefix())) return;
            String command = message.substring(room.commandPrefix().length()).toLowerCase(java.util.Locale.ROOT);
            if (!java.util.Set.of("help", "راهنما", "ping", "پینگ").contains(command)) return;
            long now = System.nanoTime();
            Long last = cooldowns.get(userId);
            if (last != null && now - last < java.util.concurrent.TimeUnit.SECONDS.toNanos(room.commandCooldownSeconds())) return;
            if (cooldowns.size() >= 10000) cooldowns.entrySet().removeIf(entry -> now - entry.getValue() > java.util.concurrent.TimeUnit.MINUTES.toNanos(10));
            if (cooldowns.size() >= 10000 && !cooldowns.containsKey(userId)) return;
            cooldowns.put(userId, now);
            response = command.equals("ping") || command.equals("پینگ") ? "Pong! 🟢" : "Hexorise | " + room.commandPrefix() + "help / راهنما | " + room.commandPrefix() + "ping / پینگ";
            if (event.path("whisper").asBoolean()) target = userId;
        }
        if (response != null && !response.isBlank()) {
            if (response.length() > 255) response = response.substring(0, 255);
            client.chat(response, target).exceptionally(failure -> { log.warn("Bot reply could not be delivered"); return null; });
        }
    }
}

package org.example.hexorise.bot;
import org.example.hexorise.client.HighriseClient;
import org.example.hexorise.room.BotAdminRepository;
import org.example.hexorise.config.HighriseProperties;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import java.util.concurrent.CompletableFuture;
@Service
public class ModerationService {
    private final BotAdminRepository admins;
    private final HighriseProperties properties;
    private final RoomDirectory directory;
    public ModerationService(BotAdminRepository admins, HighriseProperties properties, RoomDirectory directory) {
        this.admins = admins; this.properties = properties; this.directory = directory;
    }
    public CompletableFuture<Void> moderate(HighriseClient client, String actor, String target, String action, Integer seconds) {
        if (actor != null && !admins.isAdmin(properties.roomId(), actor)) throw new AccessDeniedException("Room administrator required");
        if (target.equals(directory.botId()) || admins.isAdmin(properties.roomId(), target))
            throw new AccessDeniedException("Bot and administrators are protected from moderation");
        return client.moderate(target, action, "mute".equals(action) && seconds == null ? 60 : seconds);
    }
}

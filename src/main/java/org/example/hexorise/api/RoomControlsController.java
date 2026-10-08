package org.example.hexorise.api;
import org.example.hexorise.bot.*;
import org.example.hexorise.client.HighriseClient;
import org.example.hexorise.config.HighriseProperties;
import org.example.hexorise.room.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.*;
import java.util.concurrent.CompletableFuture;
@RestController
@RequestMapping("/api/v1")
@Validated
public class RoomControlsController {
    public record AdminRequest(@NotNull BotAdminRepository.Role role) {}
    public record EmoteRequest(@NotBlank @Size(max = 100) String selector,
        @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String targetUserId, boolean repeat,
        @Min(2) @Max(300) Integer intervalSeconds) {}
    public record TargetRequest(@Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String targetUserId) {}
    public record ModerationRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String userId,
        @NotBlank @Pattern(regexp = "kick|ban|unban|mute") String action,
        @Min(1) @Max(86400) Integer durationSeconds) {}
    private final HighriseClient client;
    private final HighriseProperties properties;
    private final BotAdminRepository admins;
    private final RoomDirectory directory;
    private final EmoteService emotes;
    private final EmoteCatalog catalog;
    private final ModerationService moderation;
    private final RoomSettingsRepository settings;
    public RoomControlsController(HighriseClient client, HighriseProperties properties, BotAdminRepository admins,
        RoomDirectory directory, EmoteService emotes, EmoteCatalog catalog, ModerationService moderation, RoomSettingsRepository settings) {
        this.client = client; this.properties = properties; this.admins = admins; this.directory = directory;
        this.emotes = emotes; this.catalog = catalog; this.moderation = moderation; this.settings = settings;
    }
    @GetMapping("/emotes") public List<EmoteCatalog.Emote> catalog() { return catalog.list(); }
    @GetMapping("/bot/users") public List<RoomDirectory.RoomUser> users() { return directory.list(); }
    @GetMapping("/bot/loops") public List<EmoteService.LoopStatus> loops() { return emotes.active(); }
    @GetMapping("/rooms/{roomId}/admins")
    public List<BotAdminRepository.Administrator> admins(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String roomId) { return admins.list(roomId); }
    @PutMapping("/rooms/{roomId}/admins/{userId}")
    public Map<String, String> save(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String roomId,
        @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String userId, @Valid @RequestBody AdminRequest request) {
        admins.save(roomId, userId, request.role()); return Map.of("status", "saved");
    }
    @DeleteMapping("/rooms/{roomId}/admins/{userId}")
    public Map<String, String> remove(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String roomId,
        @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String userId) { admins.remove(roomId, userId); return Map.of("status", "removed"); }
    @PostMapping("/bot/emotes") public CompletableFuture<Map<String, String>> play(@Valid @RequestBody EmoteRequest request) {
        var room = settings.find(properties.roomId());
        if (!room.emotesEnabled()) throw new IllegalArgumentException("Emotes are disabled for this room");
        return emotes.play(client, request.selector(), request.targetUserId(), request.repeat(),
            request.intervalSeconds() == null ? room.emoteLoopIntervalSeconds() : request.intervalSeconds()).thenApply(result -> Map.of("status", "completed"));
    }
    @PostMapping("/bot/emotes/stop") public Map<String, String> stop(@Valid @RequestBody TargetRequest request) {
        emotes.stop(request.targetUserId()); return Map.of("status", "stopped");
    }
    @PostMapping("/bot/emotes/stop-all") public Map<String, String> stopAll() { emotes.stopAll(); return Map.of("status", "stopped"); }
    @PostMapping("/bot/moderation") public CompletableFuture<Map<String, String>> moderate(@Valid @RequestBody ModerationRequest request) {
        return moderation.moderate(client, null, request.userId(), request.action(), request.durationSeconds()).thenApply(result -> Map.of("status", "completed"));
    }
}

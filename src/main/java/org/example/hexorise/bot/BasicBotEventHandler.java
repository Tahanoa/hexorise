package org.example.hexorise.bot;
import tools.jackson.databind.JsonNode;
import org.example.hexorise.client.*;
import org.example.hexorise.config.HighriseProperties;
import org.example.hexorise.room.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import java.util.concurrent.*;
@Component
public class BasicBotEventHandler implements BotEventHandler {
    private static final Logger log = LoggerFactory.getLogger(BasicBotEventHandler.class);
    private static final Set<String> COMMANDS = Set.of("help", "ping", "emotes", "emote", "dance", "stop", "botdance", "botstop", "kick", "ban", "unban", "mute", "admin");
    private final RoomSettingsRepository settings;
    private final HighriseProperties properties;
    private final BotAdminRepository admins;
    private final RoomDirectory directory;
    private final SpamGuard spam;
    private final EmoteCatalog catalog;
    private final EmoteService emotes;
    private final ModerationService moderation;
    private final Map<String, Long> cooldowns = new HashMap<>();
    public BasicBotEventHandler(RoomSettingsRepository settings, HighriseProperties properties, BotAdminRepository admins,
        RoomDirectory directory, SpamGuard spam, EmoteCatalog catalog, EmoteService emotes, ModerationService moderation) {
        this.settings = settings; this.properties = properties; this.admins = admins; this.directory = directory;
        this.spam = spam; this.catalog = catalog; this.emotes = emotes; this.moderation = moderation;
    }
    @Override public void onSessionStarted(String botUserId, HighriseClient client) {
        directory.session(botUserId); cooldowns.clear(); spam.clear(); emotes.stopAll();
        client.roomUsers(directory::sync).exceptionally(failure -> { log.warn("Room roster could not be synchronized"); return null; });
    }
    @Override public void onSessionEnded(HighriseClient client) { directory.clear(); cooldowns.clear(); spam.clear(); emotes.stopAll(); }
    @Override public void handle(JsonNode event, String botUserId, HighriseClient client) {
        String type = event.path("_type").asText();
        String userId = event.path("user").path("id").asText();
        if (userId.isBlank() || userId.equals(botUserId)) return;
        if ("UserLeftEvent".equals(type)) { directory.remove(userId); cooldowns.remove(userId); emotes.stop(userId); return; }
        if (!"ChatEvent".equals(type) && !"UserJoinedEvent".equals(type)) return;
        directory.add(event.path("user"));
        RoomSettings room = settings.find(properties.roomId());
        if ("UserJoinedEvent".equals(type)) {
            if (room.welcomeEnabled()) reply(client, room.welcomeMessage().replace("{username}", event.path("user").path("username").asText()), null);
            return;
        }
        String message = event.path("message").asText().strip();
        String[] arguments = message.startsWith(room.commandPrefix()) ? message.substring(room.commandPrefix().length()).strip().split("\\s+") : new String[0];
        String command = arguments.length > 0 ? BotTexts.command(arguments[0]) : "";
        // A user can always stop their own scheduled loop, even while chat commands are blocked.
        if ("stop".equals(command)) emotes.stop(userId);
        boolean administrator = admins.isAdmin(properties.roomId(), userId);
        SpamGuard.Verdict verdict = administrator ? SpamGuard.Verdict.ALLOW : spam.check(userId, message, room);
        if (verdict != SpamGuard.Verdict.ALLOW) {
            if (verdict == SpamGuard.Verdict.NEW_BLOCK) {
                reply(client, BotTexts.text("spam", room.spamBlockSeconds()), userId);
                if (room.autoMuteEnabled()) moderation.moderate(client, null, userId, "mute", room.autoMuteSeconds())
                    .exceptionally(failure -> { log.warn("Automatic mute was rejected or unavailable"); return null; });
            }
            return;
        }
        if (!COMMANDS.contains(command)) return;
        long now = System.nanoTime(); Long last = cooldowns.get(userId);
        if (last != null && now - last < TimeUnit.SECONDS.toNanos(room.commandCooldownSeconds())) return;
        if (cooldowns.size() >= 10000) cooldowns.entrySet().removeIf(entry -> now - entry.getValue() > TimeUnit.MINUTES.toNanos(10));
        if (cooldowns.size() >= 10000 && !cooldowns.containsKey(userId)) return;
        cooldowns.put(userId, now);
        String target = event.path("whisper").asBoolean() ? userId : null;
        try {
            switch (command) {
                case "help" -> reply(client, BotTexts.text("help", room.commandPrefix()), target);
                case "ping" -> reply(client, BotTexts.text("pong"), target);
                case "emotes" -> reply(client, catalog.page(arguments.length > 1 ? Integer.parseInt(arguments[1]) : 1), userId);
                case "stop" -> reply(client, BotTexts.text("stopped"), userId);
                case "botstop" -> { requireAdmin(administrator); emotes.stop(null); reply(client, BotTexts.text("stopped"), userId); }
                case "emote", "dance", "botdance" -> {
                    if (!room.emotesEnabled()) throw new IllegalArgumentException("Emotes are disabled for this room");
                    if ("botdance".equals(command)) requireAdmin(administrator);
                    requireArguments(arguments, 2);
                    int interval = arguments.length > 2 ? Integer.parseInt(arguments[2]) : room.emoteLoopIntervalSeconds();
                    observe(emotes.play(client, arguments[1], "botdance".equals(command) ? null : userId, !"emote".equals(command), interval), client, userId);
                }
                case "kick", "ban", "unban", "mute" -> {
                    requireAdmin(administrator); requireArguments(arguments, 2);
                    String account = directory.resolve(arguments[1]);
                    Integer seconds = arguments.length > 2 ? Integer.valueOf(arguments[2]) : null;
                    observe(moderation.moderate(client, userId, account, command, seconds), client, userId);
                }
                case "admin" -> {
                    if (!admins.isOwner(properties.roomId(), userId)) throw new AccessDeniedException(BotTexts.text("owner"));
                    requireArguments(arguments, 3); String account = directory.resolve(arguments[2]);
                    if (admins.isOwner(properties.roomId(), account)) throw new AccessDeniedException("Manage owner roles through the web panel");
                    if ("add".equalsIgnoreCase(arguments[1])) { admins.save(properties.roomId(), account, BotAdminRepository.Role.ADMIN); reply(client, BotTexts.text("admin.saved"), userId); }
                    else if ("remove".equalsIgnoreCase(arguments[1])) { admins.remove(properties.roomId(), account); reply(client, BotTexts.text("admin.removed"), userId); }
                    else throw new IllegalArgumentException("Unknown administrator action");
                }
                default -> { }
            }
        } catch (AccessDeniedException denied) { reply(client, BotTexts.text("permission"), userId); }
        catch (IllegalStateException unavailable) { reply(client, BotTexts.text("failed"), userId); }
        catch (IllegalArgumentException invalid) { reply(client, BotTexts.text("usage"), userId); }
    }
    private void requireAdmin(boolean administrator) { if (!administrator) throw new AccessDeniedException("Room administrator required"); }
    private void requireArguments(String[] arguments, int count) { if (arguments.length < count) throw new IllegalArgumentException("Missing arguments"); }
    private void observe(CompletableFuture<Void> request, HighriseClient client, String userId) {
        request.whenComplete((result, failure) -> { if (failure != null && !(failure instanceof CancellationException)) reply(client, BotTexts.text("failed"), userId); });
    }
    private void reply(HighriseClient client, String response, String target) {
        if (response == null || response.isBlank()) return;
        if (response.length() > 255) response = response.substring(0, 255);
        client.chat(response, target).exceptionally(failure -> { log.warn("Bot reply could not be delivered"); return null; });
    }
}

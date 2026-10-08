package org.example.hexorise.room;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "room_settings")
public class RoomSettingsEntity {
    @Id @Column(name = "room_id", length = 128) private String roomId;
    @Column(name = "welcome_enabled", nullable = false) private boolean welcomeEnabled;
    @Column(name = "welcome_message", length = 255, nullable = false) private String welcomeMessage;
    @Column(name = "command_prefix", length = 8, nullable = false) private String commandPrefix;
    @Column(name = "command_cooldown_seconds", nullable = false) private int commandCooldownSeconds;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "anti_spam_enabled", nullable = false) private boolean antiSpamEnabled;
    @Column(name = "spam_window_seconds", nullable = false) private int spamWindowSeconds;
    @Column(name = "spam_max_messages", nullable = false) private int spamMaxMessages;
    @Column(name = "spam_max_repeats", nullable = false) private int spamMaxRepeats;
    @Column(name = "spam_block_seconds", nullable = false) private int spamBlockSeconds;
    @Column(name = "auto_mute_enabled", nullable = false) private boolean autoMuteEnabled;
    @Column(name = "auto_mute_seconds", nullable = false) private int autoMuteSeconds;
    @Column(name = "emotes_enabled", nullable = false) private boolean emotesEnabled;
    @Column(name = "emote_loop_interval_seconds", nullable = false) private int emoteLoopIntervalSeconds;
    @Version @Column(nullable = false) private Long version;
    protected RoomSettingsEntity() {}
    public RoomSettingsEntity(String roomId, RoomSettings settings) { this.roomId = roomId; update(settings); }
    public void update(RoomSettings settings) {
        welcomeEnabled = settings.welcomeEnabled(); welcomeMessage = settings.welcomeMessage();
        commandPrefix = settings.commandPrefix(); commandCooldownSeconds = settings.commandCooldownSeconds();
        antiSpamEnabled = settings.antiSpamEnabled();
        spamWindowSeconds = settings.spamWindowSeconds();
        spamMaxMessages = settings.spamMaxMessages();
        spamMaxRepeats = settings.spamMaxRepeats();
        spamBlockSeconds = settings.spamBlockSeconds();
        autoMuteEnabled = settings.autoMuteEnabled();
        autoMuteSeconds = settings.autoMuteSeconds();
        emotesEnabled = settings.emotesEnabled();
        emoteLoopIntervalSeconds = settings.emoteLoopIntervalSeconds();
        updatedAt = Instant.now();
    }
    public RoomSettings settings() { return new RoomSettings(welcomeEnabled, welcomeMessage, commandPrefix, commandCooldownSeconds, antiSpamEnabled, spamWindowSeconds, spamMaxMessages, spamMaxRepeats, spamBlockSeconds, autoMuteEnabled, autoMuteSeconds, emotesEnabled, emoteLoopIntervalSeconds); }
}

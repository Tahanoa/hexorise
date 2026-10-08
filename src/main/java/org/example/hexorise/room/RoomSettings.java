package org.example.hexorise.room;
import jakarta.validation.constraints.*;
import org.example.hexorise.bot.BotTexts;
public record RoomSettings(boolean welcomeEnabled,
    @NotBlank @Size(max = 255) String welcomeMessage,
    @NotBlank @Size(max = 8) @Pattern(regexp = "\\S+") String commandPrefix,
    @Min(1) @Max(300) int commandCooldownSeconds,
    Boolean antiSpamEnabled,
    @Min(1) @Max(120) Integer spamWindowSeconds,
    @Min(2) @Max(100) Integer spamMaxMessages,
    @Min(1) @Max(20) Integer spamMaxRepeats,
    @Min(1) @Max(600) Integer spamBlockSeconds,
    Boolean autoMuteEnabled,
    @Min(1) @Max(3600) Integer autoMuteSeconds,
    Boolean emotesEnabled,
    @Min(2) @Max(300) Integer emoteLoopIntervalSeconds) {
    public RoomSettings {
        antiSpamEnabled = antiSpamEnabled == null ? true : antiSpamEnabled;
        spamWindowSeconds = spamWindowSeconds == null ? 10 : spamWindowSeconds;
        spamMaxMessages = spamMaxMessages == null ? 6 : spamMaxMessages;
        spamMaxRepeats = spamMaxRepeats == null ? 3 : spamMaxRepeats;
        spamBlockSeconds = spamBlockSeconds == null ? 20 : spamBlockSeconds;
        autoMuteEnabled = autoMuteEnabled == null ? false : autoMuteEnabled;
        autoMuteSeconds = autoMuteSeconds == null ? 60 : autoMuteSeconds;
        emotesEnabled = emotesEnabled == null ? true : emotesEnabled;
        emoteLoopIntervalSeconds = emoteLoopIntervalSeconds == null ? 10 : emoteLoopIntervalSeconds;
    }
    public RoomSettings(boolean enabled, String message, String prefix, int cooldown) {
        this(enabled, message, prefix, cooldown, null, null, null, null, null, null, null, null, null);
    }
    public static RoomSettings defaults() { return new RoomSettings(true, BotTexts.text("welcome"), "!", 3); }
}

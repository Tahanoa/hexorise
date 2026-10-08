package org.example.hexorise.room;
import jakarta.validation.constraints.*;
public record RoomSettings(boolean welcomeEnabled,
    @NotBlank @Size(max = 255) String welcomeMessage,
    @NotBlank @Size(max = 8) @Pattern(regexp = "\\S+") String commandPrefix,
    @Min(1) @Max(300) int commandCooldownSeconds) {
    public static RoomSettings defaults() { return new RoomSettings(true, "سلام {username}، خوش آمدی!", "!", 3); }
}

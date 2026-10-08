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
    @Version @Column(nullable = false) private Long version;
    protected RoomSettingsEntity() {}
    public RoomSettingsEntity(String roomId, RoomSettings settings) { this.roomId = roomId; update(settings); }
    public void update(RoomSettings settings) {
        welcomeEnabled = settings.welcomeEnabled(); welcomeMessage = settings.welcomeMessage();
        commandPrefix = settings.commandPrefix(); commandCooldownSeconds = settings.commandCooldownSeconds();
        updatedAt = Instant.now();
    }
    public RoomSettings settings() { return new RoomSettings(welcomeEnabled, welcomeMessage, commandPrefix, commandCooldownSeconds); }
}

package org.example.hexorise.config;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;
@ConfigurationProperties("hexora.highrise")
@Validated
public class HighriseProperties {
    private final boolean enabled;
    private final String initialRoomId;
    private final String apiToken;
    private volatile String activeRoomId;
    @ConstructorBinding
    public HighriseProperties(boolean enabled, String roomId, String apiToken) {
        this.enabled = enabled; this.initialRoomId = roomId; this.activeRoomId = roomId; this.apiToken = apiToken;
    }
    public boolean enabled() { return enabled; }
    public String roomId() { return activeRoomId; }
    public String initialRoomId() { return initialRoomId; }
    public String apiToken() { return apiToken; }
    public void selectRoom(String roomId) { this.activeRoomId = roomId; }
    @AssertTrue(message = "HIGHRISE_ROOM_ID and HIGHRISE_API_TOKEN are required when enabled")
    public boolean isCredentialsValid() {
        return !enabled || (initialRoomId != null && !initialRoomId.isBlank() && apiToken != null && !apiToken.isBlank());
    }
    @Override public String toString() { return "HighriseProperties[enabled=" + enabled + ", apiToken=REDACTED]"; }
}

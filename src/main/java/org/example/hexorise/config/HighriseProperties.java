package org.example.hexorise.config;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@ConfigurationProperties("hexora.highrise")
@Validated
public record HighriseProperties(boolean enabled, String roomId, String apiToken) {
    @AssertTrue(message = "HIGHRISE_ROOM_ID and HIGHRISE_API_TOKEN are required when enabled")
    public boolean isCredentialsValid() {
        return !enabled || (roomId != null && !roomId.isBlank() && apiToken != null && !apiToken.isBlank());
    }
    @Override public String toString() { return "HighriseProperties[enabled=" + enabled + ", apiToken=REDACTED]"; }
}

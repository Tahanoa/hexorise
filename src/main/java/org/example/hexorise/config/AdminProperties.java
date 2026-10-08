package org.example.hexorise.config;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@ConfigurationProperties("hexora.admin")
@Validated
public record AdminProperties(@NotBlank String username, @NotBlank @Size(min = 16) String password) {
    @Override public String toString() { return "AdminProperties[username=" + username + ", password=REDACTED]"; }
}

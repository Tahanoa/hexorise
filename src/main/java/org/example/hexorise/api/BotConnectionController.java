package org.example.hexorise.api;
import org.example.hexorise.connection.BotConnectionService;
import org.example.hexorise.client.HighriseClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/bot")
public class BotConnectionController {
    public record ConnectionRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,128}") String roomId,
        @Size(max = 1024) @Pattern(regexp = "[!-~]*") String apiToken, boolean autoConnect) {
        @Override public String toString() { return "ConnectionRequest[roomId=" + roomId + ", apiToken=REDACTED]"; }
    }
    private final BotConnectionService settings;
    private final HighriseClient client;
    public BotConnectionController(BotConnectionService settings, HighriseClient client) { this.settings = settings; this.client = client; }
    @GetMapping("/connection") public BotConnectionService.Settings settings() { return settings.settings(); }
    @PutMapping("/connection") public synchronized BotConnectionService.Settings save(@Valid @RequestBody ConnectionRequest request) {
        return settings.save(request.roomId(), request.apiToken(), request.autoConnect());
    }
    @PostMapping("/connect") public synchronized HighriseClient.Status connect(@Valid @RequestBody ConnectionRequest request) {
        settings.save(request.roomId(), request.apiToken(), request.autoConnect());
        var credentials = settings.credentials(); client.connect(credentials.roomId(), credentials.token()); return client.status();
    }
    @PostMapping("/disconnect") public synchronized HighriseClient.Status disconnect() {
        settings.disableAutomaticConnection(); client.stop(); return client.status();
    }
    @DeleteMapping("/connection") public synchronized BotConnectionService.Settings forget() {
        settings.forgetToken(); client.stop(); return settings.settings();
    }
}

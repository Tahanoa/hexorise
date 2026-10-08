package org.example.hexorise.api;
import org.example.hexorise.client.HighriseClient;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/v1")
public class BotStatusController {
    private final HighriseClient client;
    public BotStatusController(HighriseClient client) { this.client = client; }
    @GetMapping("/bot/status") public HighriseClient.Status status() { return client.status(); }
    @GetMapping("/csrf") public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}

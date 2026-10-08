package org.example.hexorise.connection;
import org.example.hexorise.config.HighriseProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class BotConnectionService {
    public record Settings(String roomId, boolean tokenConfigured, boolean autoConnect) {}
    public record Credentials(String roomId, String token, boolean autoConnect) {
        @Override public String toString() { return "Credentials[roomId=" + roomId + ", token=REDACTED]"; }
    }
    private final BotConnectionRepository repository;
    private final TokenCipher cipher;
    private final HighriseProperties defaults;
    public BotConnectionService(BotConnectionRepository repository, TokenCipher cipher, HighriseProperties defaults) {
        this.repository = repository; this.cipher = cipher; this.defaults = defaults;
    }
    @Transactional(readOnly = true) public Settings settings() {
        return repository.findById("primary").map(entity -> new Settings(entity.roomId(), entity.encryptedToken() != null, entity.autoConnect()))
            .orElseGet(() -> new Settings(defaults.initialRoomId(), hasToken(defaults.apiToken()), defaults.enabled()));
    }
    @Transactional(readOnly = true) public Credentials credentials() {
        return repository.findById("primary").map(entity -> new Credentials(entity.roomId(), entity.encryptedToken() == null ? null : cipher.decrypt(entity.encryptedToken()), entity.autoConnect()))
            .orElseGet(() -> new Credentials(defaults.initialRoomId(), defaults.apiToken(), defaults.enabled()));
    }
    @Transactional public Settings save(String room, String token, boolean automatic) {
        var existing = repository.findById("primary"); var entity = existing.orElseGet(BotConnectionEntity::new);
        String encrypted = entity.encryptedToken();
        if (hasToken(token)) encrypted = cipher.encrypt(token);
        else if (existing.isEmpty() && hasToken(defaults.apiToken())) encrypted = cipher.encrypt(defaults.apiToken());
        if (automatic && encrypted == null) throw new IllegalArgumentException("Enter a bot API token before enabling automatic connection");
        entity.configure(room, encrypted, automatic); repository.saveAndFlush(entity);
        return new Settings(room, encrypted != null, automatic);
    }
    @Transactional public void disableAutomaticConnection() {
        var entity = repository.findById("primary").orElseGet(BotConnectionEntity::new);
        if (entity.encryptedToken() == null && hasToken(defaults.apiToken())) entity.configure(defaults.initialRoomId() == null ? "" : defaults.initialRoomId(), cipher.encrypt(defaults.apiToken()), false);
        else entity.configure(entity.roomId(), entity.encryptedToken(), false);
        repository.saveAndFlush(entity);
    }
    @Transactional public void forgetToken() {
        var entity = repository.findById("primary").orElseGet(BotConnectionEntity::new);
        entity.configure(entity.roomId(), null, false); repository.saveAndFlush(entity);
    }
    private boolean hasToken(String token) { return token != null && !token.isBlank(); }
}

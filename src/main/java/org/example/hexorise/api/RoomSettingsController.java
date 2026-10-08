package org.example.hexorise.api;
import org.example.hexorise.room.*;
import org.example.hexorise.bot.EmoteService;
import org.example.hexorise.config.HighriseProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/rooms")
@Validated
public class RoomSettingsController {
    private final RoomSettingsRepository repository;
    private final EmoteService emotes;
    private final HighriseProperties properties;
    public RoomSettingsController(RoomSettingsRepository repository, EmoteService emotes, HighriseProperties properties) {
        this.repository = repository; this.emotes = emotes; this.properties = properties;
    }
    @GetMapping("/{roomId}/settings")
    public RoomSettings get(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,128}") String roomId) { return repository.find(roomId); }
    @PutMapping("/{roomId}/settings")
    public RoomSettings put(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,128}") String roomId,
                            @Valid @RequestBody RoomSettings settings) {
        RoomSettings saved = repository.save(roomId, settings);
        if (roomId.equals(properties.roomId()) && !saved.emotesEnabled()) emotes.stopAll();
        return saved;
    }
}

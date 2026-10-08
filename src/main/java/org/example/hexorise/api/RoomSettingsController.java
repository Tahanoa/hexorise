package org.example.hexorise.api;
import org.example.hexorise.room.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/rooms")
@Validated
public class RoomSettingsController {
    private final RoomSettingsRepository repository;
    public RoomSettingsController(RoomSettingsRepository repository) { this.repository = repository; }
    @GetMapping("/{roomId}/settings")
    public RoomSettings get(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,128}") String roomId) { return repository.find(roomId); }
    @PutMapping("/{roomId}/settings")
    public RoomSettings put(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,128}") String roomId,
                            @Valid @RequestBody RoomSettings settings) { return repository.save(roomId, settings); }
}

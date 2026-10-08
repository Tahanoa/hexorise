package org.example.hexorise.bot;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
@Component
public class EmoteCatalog {
    public record Emote(int number, String name, String id) {}
    private final List<Emote> emotes;
    public EmoteCatalog(ObjectMapper mapper) throws IOException {
        try (var stream = getClass().getResourceAsStream("/emotes.json")) {
            if (stream == null) throw new IOException("Emote catalog is missing");
            emotes = List.copyOf(Arrays.asList(mapper.readValue(stream, Emote[].class)));
        }
    }
    public List<Emote> list() { return emotes; }
    public Emote resolve(String selector) {
        return emotes.stream().filter(emote -> Integer.toString(emote.number()).equals(selector)
            || emote.name().equalsIgnoreCase(selector) || emote.id().equalsIgnoreCase(selector)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown emote. Select a catalog name or number."));
    }
}

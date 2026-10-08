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
        if (selector == null) throw new IllegalArgumentException("Select an emote name, number or ID.");
        String selected = selector.strip().toLowerCase(Locale.ROOT);
        return emotes.stream().filter(emote -> Integer.toString(emote.number()).equals(selected)
            || emote.name().equalsIgnoreCase(selected) || emote.id().equalsIgnoreCase(selected)).findFirst()
            .orElseGet(() -> {
                if (selected.length() <= 100 && selected.matches("(?:dance|emote|emoji|idle)[-_][a-z0-9_-]+"))
                    return new Emote(0, selected, selected);
                throw new IllegalArgumentException("Unknown emote. Select a catalog name, number or direct Highrise ID.");
            });
    }
    public String page(int number) {
        int size = 5, pages = Math.max(1, (emotes.size() + size - 1) / size);
        if (number < 1 || number > pages) throw new IllegalArgumentException("Emote page must be between 1 and " + pages);
        String entries = emotes.stream().skip((long) (number - 1) * size).limit(size)
            .map(emote -> emote.number() + ": " + emote.name())
            .collect(java.util.stream.Collectors.joining(" | "));
        return "Emotes " + number + "/" + pages + " | " + entries;
    }
}

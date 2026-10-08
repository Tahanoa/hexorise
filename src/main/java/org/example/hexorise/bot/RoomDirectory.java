package org.example.hexorise.bot;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class RoomDirectory {
    public record RoomUser(String id, String username) {}
    private final Map<String, RoomUser> users = new ConcurrentHashMap<>();
    private volatile String botId;
    public void session(String id) { users.clear(); botId = id; }
    public String botId() { return botId; }
    public void clear() { users.clear(); botId = null; }
    public void add(JsonNode user) {
        String id = user.path("id").asText();
        if (!id.isBlank() && users.size() < 10000) users.put(id, new RoomUser(id, user.path("username").asText()));
    }
    public void remove(String id) { users.remove(id); }
    public void sync(JsonNode response) {
        for (JsonNode entry : response.path("content")) if (entry.isArray() && entry.size() > 0) add(entry.get(0));
    }
    public List<RoomUser> list() { return users.values().stream().sorted(Comparator.comparing(RoomUser::username)).toList(); }
    public String resolve(String selector) {
        if (!selector.startsWith("@")) {
            if (selector.matches("[A-Za-z0-9_-]{1,128}")) return selector;
            throw new IllegalArgumentException("Invalid user ID");
        }
        String value = selector.substring(1);
        var matches = users.values().stream().filter(user -> user.username().equalsIgnoreCase(value)).toList();
        if (matches.size() == 1) return matches.get(0).id();
        if (matches.size() > 1) throw new IllegalArgumentException("Ambiguous username. Use an account ID.");
        throw new IllegalArgumentException("Unknown account. Use a user ID or a current @username.");
    }
}

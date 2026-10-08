package org.example.hexorise.bot;
import org.example.hexorise.room.RoomSettings;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.concurrent.TimeUnit;
@Component
public class SpamGuard {
    public enum Verdict { ALLOW, BLOCKED, NEW_BLOCK }
    private record Message(long time, String text) {}
    private static class State { final Deque<Message> messages = new ArrayDeque<>(); long blockedUntil; long lastSeen; }
    private final Map<String, State> states = new HashMap<>();
    private final LongSupplier clock;
    public SpamGuard() { this(System::nanoTime); }
    SpamGuard(LongSupplier clock) { this.clock = clock; }
    public synchronized Verdict check(String userId, String message, RoomSettings settings) {
        if (!settings.antiSpamEnabled()) return Verdict.ALLOW;
        long now = clock.getAsLong();
        if (states.size() >= 10000) states.entrySet().removeIf(entry -> now - entry.getValue().lastSeen > TimeUnit.MINUTES.toNanos(12));
        if (states.size() >= 10000 && !states.containsKey(userId)) return Verdict.BLOCKED;
        State state = states.computeIfAbsent(userId, key -> new State());
        state.lastSeen = now;
        if (now < state.blockedUntil) return Verdict.BLOCKED;
        long cutoff = now - TimeUnit.SECONDS.toNanos(settings.spamWindowSeconds());
        while (!state.messages.isEmpty() && state.messages.peekFirst().time() < cutoff) state.messages.removeFirst();
        String normalized = message.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (normalized.length() > 255) normalized = normalized.substring(0, 255);
        final String text = normalized;
        state.messages.addLast(new Message(now, text));
        long repeats = state.messages.stream().filter(entry -> entry.text().equals(text)).count();
        if (state.messages.size() > settings.spamMaxMessages() || repeats > settings.spamMaxRepeats()) {
            state.blockedUntil = now + TimeUnit.SECONDS.toNanos(settings.spamBlockSeconds());
            state.messages.clear();
            return Verdict.NEW_BLOCK;
        }
        return Verdict.ALLOW;
    }
    public synchronized void clear() { states.clear(); }
}

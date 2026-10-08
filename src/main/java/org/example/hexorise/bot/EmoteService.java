package org.example.hexorise.bot;
import org.example.hexorise.client.HighriseClient;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
@Service
public class EmoteService {
    public record LoopStatus(String targetUserId, String emoteName, int intervalSeconds) {}
    private static class Loop {
        final String target; final EmoteCatalog.Emote emote; final int interval;
        CompletableFuture<Void> request; ScheduledFuture<?> next;
        Loop(String target, EmoteCatalog.Emote emote, int interval) { this.target = target; this.emote = emote; this.interval = interval; }
    }
    private final Map<String, Loop> loops = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "emote-loops"); thread.setDaemon(true); return thread;
    });
    private final EmoteCatalog catalog;
    public EmoteService(EmoteCatalog catalog) { this.catalog = catalog; }
    private String key(String target) { return target == null ? "_bot" : target; }
    public synchronized CompletableFuture<Void> play(HighriseClient client, String selector, String target, boolean repeat, int interval) {
        var emote = catalog.resolve(selector);
        if (interval < 2 || interval > 300) throw new IllegalArgumentException("Loop interval must be 2-300 seconds");
        if (target != null && !target.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalArgumentException("Invalid target ID");
        stop(target);
        if (!repeat) return client.emote(emote.id(), target);
        if (loops.size() >= 50) throw new IllegalStateException("Maximum active loops reached");
        var loop = new Loop(target, emote, interval); loops.put(key(target), loop);
        return dispatch(client, loop);
    }
    private synchronized CompletableFuture<Void> dispatch(HighriseClient client, Loop loop) {
        if (loops.get(key(loop.target)) != loop) return CompletableFuture.failedFuture(new CancellationException("Loop stopped"));
        loop.request = client.emote(loop.emote.id(), loop.target);
        loop.request.whenComplete((result, failure) -> {
            synchronized (this) {
                if (loops.get(key(loop.target)) != loop) return;
                if (failure != null) { loops.remove(key(loop.target)); return; }
                loop.next = scheduler.schedule(() -> dispatch(client, loop), loop.interval, TimeUnit.SECONDS);
            }
        });
        return loop.request;
    }
    public synchronized void stop(String target) {
        var loop = loops.remove(key(target));
        if (loop != null) {
            if (loop.next != null) loop.next.cancel(false);
            if (loop.request != null) loop.request.cancel(false);
        }
    }
    public synchronized List<LoopStatus> active() {
        return loops.values().stream().map(loop -> new LoopStatus(loop.target, loop.emote.name(), loop.interval)).toList();
    }
    public synchronized void stopAll() { new ArrayList<>(loops.values()).forEach(loop -> stop(loop.target)); }
    @PreDestroy public void close() { stopAll(); scheduler.shutdownNow(); }
}

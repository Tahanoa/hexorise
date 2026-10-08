package org.example.hexorise.client;
import tools.jackson.databind.*;
import tools.jackson.databind.node.ObjectNode;
import org.example.hexorise.config.HighriseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Single-room foundation. All event processing and application sends run on one worker. */
@Component
public class HighriseClient implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(HighriseClient.class);
    private static final URI ENDPOINT = URI.create("wss://highrise.game/web/botapi?events=chat,user_joined,user_left");
    private final HighriseProperties properties;
    private final ObjectMapper mapper;
    private final BotEventHandler events;
    private final HttpClient http;
    private final ArrayBlockingQueue<Outbound> outgoing = new ArrayBlockingQueue<>(100);
    private volatile boolean running;
    private volatile String state = "DISABLED";
    private volatile String connectionId;
    private volatile WebSocket socket;
    private volatile CompletableFuture<WebSocket> connecting;
    private Thread worker;
    record Outbound(ObjectNode payload, CompletableFuture<Void> result) {}
    record Pending(CompletableFuture<Void> result, long deadline) {}
    public record Status(String state, String roomId, String connectionId, int queuedMessages) {}
    @Autowired
    public HighriseClient(HighriseProperties properties, ObjectMapper mapper, BotEventHandler events) {
        this(properties, mapper, events, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }
    HighriseClient(HighriseProperties properties, ObjectMapper mapper, BotEventHandler events, HttpClient http) {
        this.properties = properties; this.mapper = mapper; this.events = events; this.http = http;
    }
    public Status status() { return new Status(state, properties.roomId(), connectionId, outgoing.size()); }
    public CompletableFuture<Void> chat(String message, String whisperTarget) {
        ObjectNode payload = HighriseProtocol.chat(mapper, message, whisperTarget);
        var result = new CompletableFuture<Void>();
        synchronized (outgoing) {
            if (!"READY".equals(state)) result.completeExceptionally(new IllegalStateException("Bot is not ready"));
            else if (!outgoing.offer(new Outbound(payload, result))) result.completeExceptionally(new RejectedExecutionException("Send queue is full"));
        }
        return result;
    }
    @Override public synchronized void start() {
        if (running || !properties.enabled()) return;
        running = true;
        worker = new Thread(this::run, "highrise-connection");
        worker.setDaemon(true);
        worker.start();
    }
    @Override public synchronized void stop() {
        running = false;
        if (connecting != null) connecting.cancel(true);
        if (socket != null) socket.abort();
        if (worker != null) worker.interrupt();
        state = "STOPPED";
        failQueued();
    }
    @Override public boolean isRunning() { return running; }
    @Override public boolean isAutoStartup() { return properties.enabled(); }
    private void run() {
        long backoff = 1000;
        try {
            while (running) {
                Connection listener = new Connection();
                Map<String, Pending> pending = new HashMap<>();
                state = "CONNECTING";
                try {
                    connecting = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                        .header("room-id", properties.roomId()).header("api-token", properties.apiToken())
                        .header("user-agent", "hexorise-java-bot/0.1.0")
                        .buildAsync(ENDPOINT, listener);
                    socket = connecting.get(12, TimeUnit.SECONDS);
                    connecting = null;
                    long opened = System.nanoTime(), heartbeat = opened, lastSend = 0, interval = 1000;
                    String botUserId = null;
                    while (running && !listener.closed) {
                        JsonNode event = listener.incoming.poll(100, TimeUnit.MILLISECONDS);
                        long now = System.nanoTime();
                        if (event != null) {
                            String type = event.path("_type").asText();
                            String rid = event.path("rid").asText("");
                            if (!rid.isEmpty()) {
                                Pending request = pending.remove(rid);
                                if (request != null) {
                                    if (!"ChatResponse".equals(type)) request.result().completeExceptionally(new IllegalStateException("Highrise rejected request"));
                                    else request.result().complete(null);
                                }
                            } else if ("SessionMetadata".equals(type)) {
                                if (botUserId != null || event.path("user_id").asText().isBlank()) throw new IllegalStateException("Invalid session");
                                interval = HighriseProtocol.intervalMillis(event.path("rate_limits"));
                                botUserId = event.path("user_id").asText();
                                connectionId = event.path("connection_id").asText();
                                state = "READY";
                                backoff = 1000;
                                log.info("Highrise session established");
                            } else if ("Error".equals(type)) {
                                if (event.path("do_not_reconnect").asBoolean()) {
                                    running = false;
                                    state = "REJECTED";
                                    log.warn("Highrise rejected session; automatic reconnect disabled");
                                } else throw new IllegalStateException("Highrise session error");
                            } else if (botUserId != null) {
                                try { events.handle(event, botUserId, this); }
                                catch (RuntimeException failure) { log.warn("Bot event failed ({})", failure.getClass().getSimpleName()); }
                            }
                        }
                        if (now - listener.lastReceived > TimeUnit.SECONDS.toNanos(35)
                            || (botUserId == null && now - opened > TimeUnit.SECONDS.toNanos(15)))
                            throw new TimeoutException("Highrise receive timeout");
                        if (now - heartbeat >= TimeUnit.SECONDS.toNanos(15)) {
                            socket.sendText("{\"_type\":\"KeepaliveRequest\"}", true).get(10, TimeUnit.SECONDS);
                            heartbeat = now;
                        }
                        if ("READY".equals(state) && now - lastSend >= TimeUnit.MILLISECONDS.toNanos(interval)) {
                            Outbound request = outgoing.poll();
                            if (request != null) {
                                String rid = request.payload().path("rid").asText();
                                pending.put(rid, new Pending(request.result(), now + TimeUnit.SECONDS.toNanos(15)));
                                socket.sendText(request.payload().toString(), true).get(10, TimeUnit.SECONDS);
                                lastSend = now;
                            }
                        }
                        var iterator = pending.values().iterator();
                        while (iterator.hasNext()) {
                            Pending request = iterator.next();
                            if (request.deadline() < now) {
                                request.result().completeExceptionally(new TimeoutException("Highrise request timed out"));
                                iterator.remove();
                            }
                        }
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); break;
                } catch (Exception failure) {
                    Throwable cause = failure instanceof ExecutionException ? failure.getCause() : failure;
                    if (cause instanceof WebSocketHandshakeException handshake
                        && (handshake.getResponse().statusCode() == 401 || handshake.getResponse().statusCode() == 403)) {
                        running = false; state = "REJECTED";
                    }
                    log.warn("Highrise connection unavailable ({})", cause.getClass().getSimpleName());
                } finally {
                    if (running) state = "RECONNECTING";
                    if (connecting != null) connecting.cancel(true);
                    if (socket != null) socket.abort();
                    socket = null; connecting = null; connectionId = null;
                    failQueued();
                    pending.values().forEach(request -> request.result().completeExceptionally(new IllegalStateException("Connection ended")));
                }
                if (running) { Thread.sleep(backoff); backoff = Math.min(backoff * 2, 60000); }
            }
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        finally { running = false; if (!"REJECTED".equals(state)) state = "STOPPED"; failQueued(); }
    }
    private void failQueued() {
        synchronized (outgoing) {
            Outbound request;
            while ((request = outgoing.poll()) != null) request.result().completeExceptionally(new IllegalStateException("Connection ended"));
        }
    }
    private class Connection implements WebSocket.Listener {
        final ArrayBlockingQueue<JsonNode> incoming = new ArrayBlockingQueue<>(256);
        final StringBuilder fragments = new StringBuilder();
        volatile boolean closed;
        volatile long lastReceived = System.nanoTime();
        @Override public void onOpen(WebSocket webSocket) { webSocket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            lastReceived = System.nanoTime();
            fragments.append(data);
            if (fragments.length() > 1_048_576) { closed = true; webSocket.abort(); return null; }
            if (last) {
                try {
                    if (!incoming.offer(mapper.readTree(fragments.toString()))) { closed = true; webSocket.abort(); }
                } catch (Exception malformed) { closed = true; webSocket.abort(); }
                fragments.setLength(0);
            }
            webSocket.request(1);
            return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket webSocket, int code, String reason) { closed = true; return null; }
        @Override public void onError(WebSocket webSocket, Throwable failure) { closed = true; }
    }
}

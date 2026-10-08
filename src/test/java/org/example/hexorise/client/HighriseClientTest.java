package org.example.hexorise.client;
import tools.jackson.databind.ObjectMapper;
import org.example.hexorise.config.HighriseProperties;
import org.junit.jupiter.api.*;
import java.net.http.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class HighriseClientTest {
    final ObjectMapper mapper = new ObjectMapper();
    final HttpClient http = mock(HttpClient.class);
    final WebSocket.Builder builder = mock(WebSocket.Builder.class, RETURNS_SELF);
    final WebSocket socket = mock(WebSocket.class);
    final BotEventHandler handler = mock(BotEventHandler.class);
    final AtomicReference<WebSocket.Listener> listener = new AtomicReference<>();
    HighriseClient client;
    @BeforeEach void setup() {
        when(http.newWebSocketBuilder()).thenReturn(builder);
        when(builder.buildAsync(any(), any())).thenAnswer(invocation -> {
            listener.set(invocation.getArgument(1));
            listener.get().onOpen(socket);
            return CompletableFuture.completedFuture(socket);
        });
        when(socket.sendText(any(), eq(true))).thenReturn(CompletableFuture.completedFuture(socket));
        client = new HighriseClient(new HighriseProperties(true, "room", "test-api-token"), mapper, handler, http);
    }
    @AfterEach void cleanup() { client.stop(); }
    @Test void waitsForMetadataThenCorrelatesChatAndDispatchesEvents() throws Exception {
        client.start();
        await(() -> listener.get() != null);
        assertThat(client.status().state()).isEqualTo("CONNECTING");
        listener.get().onText(socket, "{\"_type\":\"SessionMeta", false);
        listener.get().onText(socket, "data\",\"user_id\":\"bot\",\"connection_id\":\"c1\",\"rate_limits\":{\"global\":[1,1]}}", true);
        await(() -> "READY".equals(client.status().state()));
        when(socket.sendText(any(), eq(true))).thenAnswer(invocation -> {
            var payload = mapper.readTree(invocation.getArgument(0).toString());
            if (payload.has("rid")) listener.get().onText(socket,
                "{\"_type\":\"ChatResponse\",\"rid\":\"" + payload.path("rid").asText() + "\"}", true);
            return CompletableFuture.completedFuture(socket);
        });
        client.chat("hello", null).get(3, TimeUnit.SECONDS);
        var chat = mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"u1\"},\"message\":\"!ping\"}");
        listener.get().onText(socket, chat.toString(), true);
        verify(handler, timeout(3000)).handle(eq(chat), eq("bot"), eq(client));
        verify(builder).header("room-id", "room");
        verify(builder).header("api-token", "test-api-token");
    }
    @Test void honorsTerminalSessionErrorAndDoesNotReconnect() throws Exception {
        client.start();
        await(() -> listener.get() != null);
        listener.get().onText(socket, "{\"_type\":\"Error\",\"message\":\"denied\",\"do_not_reconnect\":true}", true);
        await(() -> !client.isRunning());
        assertThat(client.status().state()).isEqualTo("REJECTED");
        verify(builder, times(1)).buildAsync(any(), any());
        assertThat(client.chat("hi", null)).isCompletedExceptionally();
    }
    @Test void failsPendingRequestWhenConnectionCloses() throws Exception {
        client.start();
        await(() -> listener.get() != null);
        listener.get().onText(socket, "{\"_type\":\"SessionMetadata\",\"user_id\":\"bot\",\"connection_id\":\"c1\",\"rate_limits\":{\"global\":[1,1]}}", true);
        await(() -> "READY".equals(client.status().state()));
        var request = client.chat("hi", null);
        verify(socket, timeout(3000)).sendText(contains("ChatRequest"), eq(true));
        listener.get().onClose(socket, 1000, "");
        await(request::isCompletedExceptionally);
        assertThat(client.status().state()).isEqualTo("RECONNECTING");
    }
    @Test void correlatesEmoteModerationAndRosterResponsesInSharedQueue() throws Exception {
        when(socket.sendText(any(), eq(true))).thenAnswer(invocation -> {
            var payload = mapper.readTree(invocation.getArgument(0).toString());
            if (payload.has("rid")) listener.get().onText(socket, mapper.createObjectNode()
                .put("_type", payload.path("_type").asText().replace("Request", "Response"))
                .put("rid", payload.path("rid").asText()).toString(), true);
            return CompletableFuture.completedFuture(socket);
        });
        client.start(); await(() -> listener.get() != null);
        listener.get().onText(socket, "{\"_type\":\"SessionMetadata\",\"user_id\":\"bot\",\"rate_limits\":{\"global\":[1,1]}}", true);
        await(() -> "READY".equals(client.status().state()));
        client.emote("emote-hello", "user").get(3, TimeUnit.SECONDS);
        client.moderate("user", "mute", 60).get(3, TimeUnit.SECONDS);
        var roster = new AtomicReference<tools.jackson.databind.JsonNode>();
        client.roomUsers(roster::set).get(3, TimeUnit.SECONDS);
        assertThat(roster.get().path("_type").asText()).isEqualTo("GetRoomUsersResponse");
        verify(handler).onSessionStarted("bot", client);
    }
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(20);
        assertThat(condition.getAsBoolean()).isTrue();
    }
}

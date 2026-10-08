package org.example.hexorise.bot;
import org.example.hexorise.client.HighriseClient;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class EmoteServiceTest {
    final HighriseClient client = mock(HighriseClient.class);
    EmoteService service;
    @BeforeEach void setup() throws Exception { service = new EmoteService(new EmoteCatalog(new ObjectMapper())); }
    @AfterEach void cleanup() { service.close(); }
    @Test void resolvesNameNumberAndIdentifier() throws Exception {
        var catalog = new EmoteCatalog(new ObjectMapper());
        assertThat(catalog.resolve("1")).isEqualTo(catalog.resolve("MACARENA"));
        assertThat(catalog.resolve("dance-macarena")).isEqualTo(catalog.resolve("1"));
        assertThatThrownBy(() -> catalog.resolve("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void waitsForAcknowledgementAndCancelsReplacedLoop() {
        var first = new CompletableFuture<Void>(); var replacement = new CompletableFuture<Void>();
        when(client.emote(anyString(), eq("user"))).thenReturn(first, replacement);
        service.play(client, "1", "user", true, 2);
        verify(client, after(2200).times(1)).emote(anyString(), eq("user"));
        service.play(client, "hello", "user", true, 2);
        assertThat(first).isCancelled();
        assertThat(service.active()).containsExactly(new EmoteService.LoopStatus("user", "hello", 2));
        service.stop("user");
        assertThat(replacement).isCancelled(); assertThat(service.active()).isEmpty();
    }
    @Test void successfulResponseSchedulesNextSendAndStopPreventsMore() throws Exception {
        when(client.emote(anyString(), isNull())).thenReturn(CompletableFuture.completedFuture(null));
        service.play(client, "hello", null, true, 2).get();
        verify(client, timeout(3000).times(2)).emote("emote-hello", null);
        service.stop(null);
        verify(client, after(2200).times(2)).emote("emote-hello", null);
        assertThat(service.active()).isEmpty();
    }
    @Test void rejectedRequestRemovesLoop() {
        when(client.emote(anyString(), isNull())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("offline")));
        assertThat(service.play(client, "1", null, true, 10)).isCompletedExceptionally();
        assertThat(service.active()).isEmpty();
    }
}

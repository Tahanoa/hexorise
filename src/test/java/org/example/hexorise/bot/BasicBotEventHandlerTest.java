package org.example.hexorise.bot;
import tools.jackson.databind.ObjectMapper;
import org.example.hexorise.client.HighriseClient;
import org.example.hexorise.config.HighriseProperties;
import org.example.hexorise.room.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
class BasicBotEventHandlerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RoomSettingsRepository settings = mock(RoomSettingsRepository.class);
    private final HighriseClient client = mock(HighriseClient.class);
    private final BasicBotEventHandler handler = new BasicBotEventHandler(settings, new HighriseProperties(false, "room", ""));
    @Test void repliesPrivatelyAndAppliesCooldown() throws Exception {
        when(settings.find("room")).thenReturn(RoomSettings.defaults());
        when(client.chat(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        var event = mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"u1\"},\"message\":\"!پینگ\",\"whisper\":true}");
        handler.handle(event, "bot", client);
        handler.handle(event, "bot", client);
        verify(client, times(1)).chat("Pong! 🟢", "u1");
    }
    @Test void ignoresOwnMessages() throws Exception {
        handler.handle(mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"bot\"},\"message\":\"!ping\"}"), "bot", client);
        verifyNoInteractions(client, settings);
    }
    @Test void welcomesWithCustomTemplate() throws Exception {
        when(settings.find("room")).thenReturn(new RoomSettings(true, "Hi {username}", "!", 3));
        when(client.chat(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        handler.handle(mapper.readTree("{\"_type\":\"UserJoinedEvent\",\"user\":{\"id\":\"u1\",\"username\":\"Taha\"}}"), "bot", client);
        verify(client).chat("Hi Taha", null);
    }
}

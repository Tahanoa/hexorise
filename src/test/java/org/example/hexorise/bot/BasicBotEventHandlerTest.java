package org.example.hexorise.bot;
import tools.jackson.databind.ObjectMapper;
import org.example.hexorise.client.HighriseClient;
import org.example.hexorise.config.HighriseProperties;
import org.example.hexorise.room.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
class BasicBotEventHandlerTest {
    BasicBotEventHandlerTest() throws java.io.IOException {}
    private final ObjectMapper mapper = new ObjectMapper();
    private final RoomSettingsRepository settings = mock(RoomSettingsRepository.class);
    private final HighriseClient client = mock(HighriseClient.class);
    private final BotAdminRepository admins = mock(BotAdminRepository.class);
    private final RoomDirectory directory = new RoomDirectory();
    private final EmoteService emotes = mock(EmoteService.class);
    private final BasicBotEventHandler handler = new BasicBotEventHandler(settings, new HighriseProperties(false, "room", ""), admins, directory, new SpamGuard(), new EmoteCatalog(mapper), emotes, new ModerationService(admins, new HighriseProperties(false, "room", ""), directory));
    @Test void repliesPrivatelyAndAppliesCooldown() throws Exception {
        when(settings.find("room")).thenReturn(RoomSettings.defaults());
        when(client.chat(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        var event = mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"u1\"},\"message\":\"!ping\",\"whisper\":true}");
        handler.handle(event, "bot", client);
        handler.handle(event, "bot", client);
        verify(client, times(1)).chat("Pong! \uD83D\uDFE2", "u1");
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
    @Test void rejectsModerationFromVisitors() throws Exception {
        when(settings.find("room")).thenReturn(RoomSettings.defaults());
        when(client.chat(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        handler.handle(mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"visitor\"},\"message\":\"!kick target\"}"), "bot", client);
        verify(client).chat(BotTexts.text("permission"), "visitor");
        verify(client, never()).moderate(anyString(), anyString(), any());
    }
    @Test void ownerCanPersistChatAdministrator() throws Exception {
        when(settings.find("room")).thenReturn(RoomSettings.defaults());
        when(admins.isAdmin("room", "owner")).thenReturn(true);
        when(admins.isOwner("room", "owner")).thenReturn(true);
        when(client.chat(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        handler.handle(mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"owner\"},\"message\":\"!admin add target\"}"), "bot", client);
        verify(admins).save("room", "target", BotAdminRepository.Role.ADMIN);
    }
    @Test void stopAlwaysCancelsLoopEvenDuringCooldownAndSpamBlock() throws Exception {
        when(settings.find("room")).thenReturn(RoomSettings.defaults());
        when(client.chat(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        var ping = mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"user\"},\"message\":\"!ping\"}");
        for (int i = 0; i < 5; i++) handler.handle(ping, "bot", client);
        handler.handle(mapper.readTree("{\"_type\":\"ChatEvent\",\"user\":{\"id\":\"user\"},\"message\":\"!stop\"}"), "bot", client);
        verify(emotes).stop("user");
        verify(client, times(1)).chat(BotTexts.text("spam", 20), "user");
    }
}

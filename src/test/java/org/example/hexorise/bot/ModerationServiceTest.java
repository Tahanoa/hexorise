package org.example.hexorise.bot;
import org.example.hexorise.room.BotAdminRepository;
import org.example.hexorise.config.HighriseProperties;
import org.example.hexorise.client.HighriseClient;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ModerationServiceTest {
    final BotAdminRepository admins = mock(BotAdminRepository.class);
    final HighriseClient client = mock(HighriseClient.class);
    final RoomDirectory directory = new RoomDirectory();
    final ModerationService service = new ModerationService(admins, new HighriseProperties(false, "room", ""), directory);
    @Test void rejectsUnprivilegedChatUserBeforeSending() {
        assertThatThrownBy(() -> service.moderate(client, "visitor", "target", "kick", null)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(client);
    }
    @Test void protectsBotAndSavedAdministrators() {
        directory.session("bot"); when(admins.isAdmin("room", "owner")).thenReturn(true);
        assertThatThrownBy(() -> service.moderate(client, null, "bot", "kick", null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.moderate(client, null, "owner", "ban", null)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(client);
    }
    @Test void defaultsMuteDurationAndPropagatesServerRejection() {
        when(admins.isAdmin("room", "admin")).thenReturn(true);
        when(client.moderate("target", "mute", 60)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("rejected")));
        assertThat(service.moderate(client, "admin", "target", "mute", null)).isCompletedExceptionally();
        verify(client).moderate("target", "mute", 60);
    }
}

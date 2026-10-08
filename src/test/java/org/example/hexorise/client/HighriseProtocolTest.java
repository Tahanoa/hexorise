package org.example.hexorise.client;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class HighriseProtocolTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Test void serializesChatAndWhisperWithoutCredentials() {
        var request = HighriseProtocol.chat(mapper, "سلام", "user-1");
        assertThat(request.path("_type").asText()).isEqualTo("ChatRequest");
        assertThat(request.path("message").asText()).isEqualTo("سلام");
        assertThat(request.path("whisper_target_id").asText()).isEqualTo("user-1");
        assertThat(request.path("rid").asText()).isNotBlank();
        assertThat(HighriseProtocol.chat(mapper, "hi", null).has("whisper_target_id")).isFalse();
    }
    @Test void rejectsEmptyAndOversizedChat() {
        assertThatThrownBy(() -> HighriseProtocol.chat(mapper, " ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HighriseProtocol.chat(mapper, "x".repeat(256), null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void honorsStrictestSessionBucketAndSafetyFloor() throws Exception {
        assertThat(HighriseProtocol.intervalMillis(mapper.readTree("{\"global\":[2,10],\"chat\":[1,7]}"))).isEqualTo(7000);
        assertThat(HighriseProtocol.intervalMillis(mapper.readTree("{\"global\":[100,1]}"))).isEqualTo(1000);
        assertThatThrownBy(() -> HighriseProtocol.intervalMillis(mapper.readTree("{\"global\":[0,1]}"))).isInstanceOf(IllegalArgumentException.class);
    }
}

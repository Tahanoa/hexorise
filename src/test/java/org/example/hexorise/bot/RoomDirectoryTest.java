package org.example.hexorise.bot;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class RoomDirectoryTest {
    @Test void distinguishesAccountIdsFromNamesAndRejectsAmbiguity() throws Exception {
        var directory = new RoomDirectory(); var mapper = new ObjectMapper();
        directory.add(mapper.readTree("{\"id\":\"account-id\",\"username\":\"target-id\"}"));
        assertThat(directory.resolve("target-id")).isEqualTo("target-id");
        assertThat(directory.resolve("@TARGET-ID")).isEqualTo("account-id");
        directory.add(mapper.readTree("{\"id\":\"another-id\",\"username\":\"target-id\"}"));
        assertThatThrownBy(() -> directory.resolve("@target-id")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> directory.resolve("@missing")).isInstanceOf(IllegalArgumentException.class);
        directory.clear(); assertThat(directory.list()).isEmpty();
    }
}

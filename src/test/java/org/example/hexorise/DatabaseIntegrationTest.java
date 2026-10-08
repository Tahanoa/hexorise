package org.example.hexorise;
import org.example.hexorise.room.*;
import org.example.hexorise.client.HighriseClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties = {"hexora.admin.password=integration-test-password", "hexora.highrise.enabled=false"})
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class DatabaseIntegrationTest {
    @Autowired RoomSettingsRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired HighriseClient client;
    @Test void migratesPostgresAndPersistsRoomSettings() {
        assertThat(client.status().state()).isEqualTo("DISABLED");
        assertThat(repository.find("integration-room")).isEqualTo(RoomSettings.defaults());
        var settings = new RoomSettings(false, "سلام {username}", "/", 7);
        repository.save("integration-room", settings);
        assertThat(repository.find("integration-room")).isEqualTo(settings);
        assertThat(jdbc.queryForObject("SELECT version FROM room_settings WHERE room_id = ?", Long.class, "integration-room")).isZero();
        repository.save("integration-room", RoomSettings.defaults());
        assertThat(repository.find("integration-room")).isEqualTo(RoomSettings.defaults());
        assertThat(jdbc.queryForObject("SELECT version FROM room_settings WHERE room_id = ?", Long.class, "integration-room")).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class)).isEqualTo(2);
        jdbc.update("DELETE FROM room_settings WHERE room_id = ?", "integration-room");
    }
}

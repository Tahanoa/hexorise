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
    @Autowired BotAdminRepository admins;
    @Test void migratesPostgresAndPersistsRoomSettings() {
        assertThat(client.status().state()).isEqualTo("DISABLED");
        assertThat(repository.find("integration-room")).isEqualTo(RoomSettings.defaults());
        var settings = new RoomSettings(false, "Hello {username}", "/", 7, true, 15, 9, 4, 45, true, 90, false, 25);
        repository.save("integration-room", settings);
        assertThat(repository.find("integration-room")).isEqualTo(settings);
        assertThat(jdbc.queryForObject("SELECT version FROM room_settings WHERE room_id = ?", Long.class, "integration-room")).isZero();
        repository.save("integration-room", RoomSettings.defaults());
        assertThat(repository.find("integration-room")).isEqualTo(RoomSettings.defaults());
        assertThat(jdbc.queryForObject("SELECT version FROM room_settings WHERE room_id = ?", Long.class, "integration-room")).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class)).isEqualTo(3);
        admins.save("integration-room", "owner-id", BotAdminRepository.Role.OWNER);
        assertThat(admins.isOwner("integration-room", "owner-id")).isTrue();
        assertThat(new BotAdminRepository(jdbc).list("integration-room")).containsExactly(new BotAdminRepository.Administrator("owner-id", BotAdminRepository.Role.OWNER));
        admins.remove("integration-room", "owner-id");
        assertThat(admins.isAdmin("integration-room", "owner-id")).isFalse();
        jdbc.update("DELETE FROM room_settings WHERE room_id = ?", "integration-room");
    }
}

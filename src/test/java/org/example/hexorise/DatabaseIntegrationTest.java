package org.example.hexorise;
import org.example.hexorise.room.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.AvailableSettings;
import javax.sql.DataSource;
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
    @Autowired org.example.hexorise.connection.BotConnectionService connections;
    @Autowired DataSource dataSource;
    @Autowired RoomSettingsJpaRepository rooms;
    @Test void createsPostgresTablesAndPersistsRoomSettings() {
        assertThat(client.status().state()).isEqualTo("DISABLED");
        assertThat(repository.find("integration-room")).isEqualTo(RoomSettings.defaults());
        var settings = new RoomSettings(false, "Hello {username}", "/", 7, true, 15, 9, 4, 45, true, 90, false, 25);
        repository.save("integration-room", settings);
        assertThat(repository.find("integration-room")).isEqualTo(settings);
        assertThat(jdbc.queryForObject("SELECT version FROM room_settings WHERE room_id = ?", Long.class, "integration-room")).isZero();
        repository.save("integration-room", RoomSettings.defaults());
        assertThat(repository.find("integration-room")).isEqualTo(RoomSettings.defaults());
        assertThat(jdbc.queryForObject("SELECT version FROM room_settings WHERE room_id = ?", Long.class, "integration-room")).isEqualTo(1L);
        admins.save("integration-room", "owner-id", BotAdminRepository.Role.OWNER);
        assertThat(admins.isOwner("integration-room", "owner-id")).isTrue();
        assertThat(new BotAdminRepository(jdbc, rooms).list("integration-room")).containsExactly(new BotAdminRepository.Administrator("owner-id", BotAdminRepository.Role.OWNER));
        admins.remove("integration-room", "owner-id");
        assertThat(admins.isAdmin("integration-room", "owner-id")).isFalse();
        jdbc.update("DELETE FROM room_settings WHERE room_id = ?", "integration-room");
    }
    @Test void storesEncryptedConnectionAndReusesTokenWithoutReturningSecrets() {
        try {
            connections.save("saved-room", "saved-api-token", true);
            assertThat(connections.settings()).isEqualTo(new org.example.hexorise.connection.BotConnectionService.Settings("saved-room", true, true));
            String stored = jdbc.queryForObject("SELECT encrypted_token FROM bot_connection WHERE id = 'primary'", String.class);
            assertThat(stored).doesNotContain("saved-api-token");
            connections.save("next-room", null, false);
            assertThat(connections.credentials().token()).isEqualTo("saved-api-token");
            assertThat(connections.credentials().roomId()).isEqualTo("next-room");
            connections.disableAutomaticConnection(); assertThat(connections.settings().autoConnect()).isFalse();
            connections.forgetToken(); assertThat(connections.settings().tokenConfigured()).isFalse();
            assertThat(connections.credentials().token()).isNull();
        } finally { jdbc.update("DELETE FROM bot_connection WHERE id = 'primary'"); }
    }
    @Test void updatesExistingPostgresTablesAndPreservesData() {
        try {
            jdbc.execute("CREATE SCHEMA existing_rooms_test");
            jdbc.execute("CREATE TABLE existing_rooms_test.room_settings (room_id VARCHAR(128) PRIMARY KEY, welcome_enabled BOOLEAN NOT NULL, welcome_message VARCHAR(255) NOT NULL, command_prefix VARCHAR(8) NOT NULL, command_cooldown_seconds INTEGER NOT NULL, updated_at TIMESTAMPTZ NOT NULL)");
            jdbc.execute("CREATE TABLE existing_rooms_test.bot_admins (room_id VARCHAR(128) NOT NULL REFERENCES existing_rooms_test.room_settings(room_id), user_id VARCHAR(128) NOT NULL, role VARCHAR(16) NOT NULL, created_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(room_id, user_id))");
            jdbc.update("INSERT INTO existing_rooms_test.room_settings VALUES (?, true, ?, '!', 3, CURRENT_TIMESTAMP)", "existing-room", "Original welcome {username}");
            jdbc.update("INSERT INTO existing_rooms_test.bot_admins VALUES (?, ?, 'OWNER', CURRENT_TIMESTAMP)", "existing-room", "existing-owner");
            var configuration = new Configuration().addAnnotatedClass(RoomSettingsEntity.class).addAnnotatedClass(BotAdminEntity.class)
                .setProperty("hibernate.hbm2ddl.auto", "update").setProperty("hibernate.default_schema", "existing_rooms_test");
            configuration.getProperties().put(AvailableSettings.DATASOURCE, dataSource);
            try (var factory = configuration.buildSessionFactory()) {
                assertThat(jdbc.queryForObject("SELECT welcome_message FROM existing_rooms_test.room_settings WHERE room_id = ?", String.class, "existing-room"))
                    .isEqualTo("Original welcome {username}");
                assertThat(jdbc.queryForObject("SELECT emote_loop_interval_seconds FROM existing_rooms_test.room_settings WHERE room_id = ?", Integer.class, "existing-room"))
                    .isEqualTo(10);
                assertThat(jdbc.queryForObject("SELECT role FROM existing_rooms_test.bot_admins WHERE user_id = ?", String.class, "existing-owner"))
                    .isEqualTo("OWNER");
            }
        } finally { jdbc.execute("DROP SCHEMA IF EXISTS existing_rooms_test CASCADE"); }
    }
}

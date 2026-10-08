package org.example.hexorise.room;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Repository
public class BotAdminRepository {
    public enum Role { OWNER, ADMIN }
    public record Administrator(String userId, Role role) {}
    private final JdbcTemplate jdbc;
    private final RoomSettingsJpaRepository rooms;
    public BotAdminRepository(JdbcTemplate jdbc, RoomSettingsJpaRepository rooms) { this.jdbc = jdbc; this.rooms = rooms; }
    public List<Administrator> list(String roomId) {
        return jdbc.query("SELECT user_id, role FROM bot_admins WHERE room_id = ? ORDER BY role DESC, user_id",
            (rs, n) -> new Administrator(rs.getString(1), Role.valueOf(rs.getString(2))), roomId);
    }
    public boolean isAdmin(String roomId, String userId) { return list(roomId).stream().anyMatch(admin -> admin.userId().equals(userId)); }
    public boolean isOwner(String roomId, String userId) { return list(roomId).stream().anyMatch(admin -> admin.userId().equals(userId) && admin.role() == Role.OWNER); }
    @Transactional
    public void save(String roomId, String userId, Role role) {
        if (!rooms.existsById(roomId)) rooms.saveAndFlush(new RoomSettingsEntity(roomId, RoomSettings.defaults()));
        jdbc.update("INSERT INTO bot_admins (room_id, user_id, role, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP) ON CONFLICT (room_id, user_id) DO UPDATE SET role = EXCLUDED.role", roomId, userId, role.name());
    }
    public void remove(String roomId, String userId) { jdbc.update("DELETE FROM bot_admins WHERE room_id = ? AND user_id = ?", roomId, userId); }
}

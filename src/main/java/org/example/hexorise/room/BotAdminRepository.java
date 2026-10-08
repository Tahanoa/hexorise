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
    public BotAdminRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Administrator> list(String roomId) {
        return jdbc.query("SELECT user_id, role FROM bot_admins WHERE room_id = ? ORDER BY role DESC, user_id",
            (rs, n) -> new Administrator(rs.getString(1), Role.valueOf(rs.getString(2))), roomId);
    }
    public boolean isAdmin(String roomId, String userId) { return list(roomId).stream().anyMatch(admin -> admin.userId().equals(userId)); }
    public boolean isOwner(String roomId, String userId) { return list(roomId).stream().anyMatch(admin -> admin.userId().equals(userId) && admin.role() == Role.OWNER); }
    @Transactional
    public void save(String roomId, String userId, Role role) {
        jdbc.update("INSERT INTO room_settings (room_id) VALUES (?) ON CONFLICT (room_id) DO NOTHING", roomId);
        jdbc.update("INSERT INTO bot_admins (room_id, user_id, role) VALUES (?, ?, ?) ON CONFLICT (room_id, user_id) DO UPDATE SET role = EXCLUDED.role", roomId, userId, role.name());
    }
    public void remove(String roomId, String userId) { jdbc.update("DELETE FROM bot_admins WHERE room_id = ? AND user_id = ?", roomId, userId); }
}

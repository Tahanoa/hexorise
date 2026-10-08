package org.example.hexorise.room;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
@Entity
@Table(name = "bot_admins")
@IdClass(BotAdminEntity.Key.class)
public class BotAdminEntity {
    @Id @Column(name = "room_id", length = 128) private String roomId;
    @Id @Column(name = "user_id", length = 128) private String userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private BotAdminRepository.Role role;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RoomSettingsEntity room;
    protected BotAdminEntity() {}
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        public String roomId;
        public String userId;
        public Key() {}
        @Override public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(roomId, key.roomId) && Objects.equals(userId, key.userId);
        }
        @Override public int hashCode() { return Objects.hash(roomId, userId); }
    }
}

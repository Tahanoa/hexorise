package org.example.hexorise.room;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class RoomSettingsRepository {
    private final RoomSettingsJpaRepository rooms;
    public RoomSettingsRepository(RoomSettingsJpaRepository rooms) { this.rooms = rooms; }
    @Transactional(readOnly = true)
    public RoomSettings find(String roomId) {
        return rooms.findById(roomId).map(RoomSettingsEntity::settings).orElseGet(RoomSettings::defaults);
    }
    @Transactional
    public RoomSettings save(String roomId, RoomSettings settings) {
        var entity = rooms.findById(roomId).orElseGet(() -> new RoomSettingsEntity(roomId, settings));
        entity.update(settings);
        rooms.saveAndFlush(entity);
        return settings;
    }
}

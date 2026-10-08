package org.example.hexorise.connection;
import jakarta.persistence.*;
@Entity
@Table(name = "bot_connection")
public class BotConnectionEntity {
    @Id private String id = "primary";
    @Column(name = "room_id", length = 128, nullable = false) private String roomId = "";
    @Column(name = "encrypted_token", length = 4096) private String encryptedToken;
    @Column(name = "auto_connect", nullable = false) private boolean autoConnect;
    public BotConnectionEntity() {}
    public String roomId() { return roomId; }
    public String encryptedToken() { return encryptedToken; }
    public boolean autoConnect() { return autoConnect; }
    public void configure(String room, String encrypted, boolean automatic) { roomId = room; encryptedToken = encrypted; autoConnect = automatic; }
}

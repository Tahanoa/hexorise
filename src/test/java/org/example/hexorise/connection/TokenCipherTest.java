package org.example.hexorise.connection;
import org.example.hexorise.config.AdminProperties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class TokenCipherTest {
    @Test void encryptsWithFreshRandomnessAndRejectsTamperingAndPasswordChanges() {
        var cipher = new TokenCipher(new AdminProperties("admin", "test-password-long-enough"));
        var encrypted = cipher.encrypt("secret-bot-token");
        assertThat(encrypted).doesNotContain("secret-bot-token").isNotEqualTo(cipher.encrypt("secret-bot-token"));
        assertThat(cipher.decrypt(encrypted)).isEqualTo("secret-bot-token");
        var bytes = java.util.Base64.getDecoder().decode(encrypted); bytes[bytes.length - 1] ^= 1;
        assertThatThrownBy(() -> cipher.decrypt(java.util.Base64.getEncoder().encodeToString(bytes))).isInstanceOf(IllegalArgumentException.class);
        var rotated = new TokenCipher(new AdminProperties("admin", "different-password-long-enough"));
        assertThatThrownBy(() -> rotated.decrypt(encrypted)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Enter a new API token");
        assertThat(new BotConnectionService.Credentials("room", "secret-bot-token", true).toString()).doesNotContain("secret-bot-token");
    }
}

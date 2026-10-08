package org.example.hexorise.connection;
import org.example.hexorise.config.AdminProperties;
import org.springframework.stereotype.Component;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
@Component
public class TokenCipher {
    private final char[] password;
    private final SecureRandom random = new SecureRandom();
    public TokenCipher(AdminProperties admin) { password = admin.password().toCharArray(); }
    private SecretKey key(byte[] salt) throws GeneralSecurityException {
        var spec = new PBEKeySpec(password, salt, 210000, 256);
        try { return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(), "AES"); }
        finally { spec.clearPassword(); }
    }
    public String encrypt(String token) {
        try {
            byte[] salt = new byte[16], iv = new byte[12]; random.nextBytes(salt); random.nextBytes(iv);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key(salt), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(28 + ciphertext.length).put(salt).put(iv).put(ciphertext).array());
        } catch (GeneralSecurityException failure) { throw new IllegalStateException("Could not store bot credentials"); }
    }
    public String decrypt(String encrypted) {
        try {
            var buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encrypted));
            byte[] salt = new byte[16], iv = new byte[12]; buffer.get(salt); buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()]; buffer.get(ciphertext);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(salt), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException failure) { throw new IllegalArgumentException("Saved token cannot be decrypted. Enter a new API token and save again."); }
    }
}

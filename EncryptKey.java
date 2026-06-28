import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptKey {
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    public static void main(String[] args) throws Exception {
        String secret = "cloud-brain-medical-dev-secret-change-me";
        String apiKey = "sk-9d84627a67084a05a4c505afc56fe75e8";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        // Encrypt
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        String ciphertext = Base64.getEncoder().encodeToString(payload);
        System.out.println("Encrypted: " + ciphertext);

        // Decrypt and verify
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] iv2 = new byte[GCM_IV_BYTES];
        byte[] enc2 = new byte[decoded.length - GCM_IV_BYTES];
        System.arraycopy(decoded, 0, iv2, 0, GCM_IV_BYTES);
        System.arraycopy(decoded, GCM_IV_BYTES, enc2, 0, enc2.length);
        Cipher cipher2 = Cipher.getInstance("AES/GCM/NoPadding");
        cipher2.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv2));
        byte[] decrypted = cipher2.doFinal(enc2);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        System.out.println("Decrypted: " + result);
        System.out.println("Match: " + apiKey.equals(result));
    }
}

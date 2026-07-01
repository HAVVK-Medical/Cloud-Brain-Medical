package com.cloudbrain.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class ConfigCipherTest {

    @Test
    void encryptAndDecryptRoundTripsPlaintextWithoutExposingIt() {
        ConfigCipher cipher = new ConfigCipher("unit-test-secret");

        String encrypted = cipher.encrypt("sk-test-123");

        assertThat(encrypted).isNotBlank();
        assertThat(encrypted).isNotEqualTo("sk-test-123");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("sk-test-123");
    }

    @Test
    void encryptUsesFreshIvForSamePlaintext() {
        ConfigCipher cipher = new ConfigCipher("unit-test-secret");

        String first = cipher.encrypt("same-secret");
        String second = cipher.encrypt("same-secret");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("same-secret");
        assertThat(cipher.decrypt(second)).isEqualTo("same-secret");
    }

    @Test
    void blankValuesAreStoredAsNullAndInvalidCiphertextIsRejected() {
        ConfigCipher cipher = new ConfigCipher("unit-test-secret");

        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("  ")).isNull();
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("\t")).isNull();
        assertThatThrownBy(() -> cipher.decrypt("not-base64"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unable to decrypt secret");
    }

    @Test
    void decryptRejectsPayloadWithoutCipherText() {
        ConfigCipher cipher = new ConfigCipher("unit-test-secret");
        String ivOnly = java.util.Base64.getEncoder().encodeToString(new byte[12]);

        assertThatThrownBy(() -> cipher.decrypt(ivOnly))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unable to decrypt secret");
    }

    @Test
    void encryptWrapsCipherInitializationFailures() throws Exception {
        ConfigCipher cipher = new ConfigCipher("unit-test-secret");
        Field field = ConfigCipher.class.getDeclaredField("keySpec");
        field.setAccessible(true);
        field.set(cipher, new SecretKeySpec(new byte[1], "AES"));

        assertThatThrownBy(() -> cipher.encrypt("secret"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("unable to encrypt secret");
    }
}

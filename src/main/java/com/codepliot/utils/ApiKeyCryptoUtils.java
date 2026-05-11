package com.codepliot.utils;

import com.codepliot.config.SecretProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.ErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyCryptoUtils {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String CIPHER = "AES/GCM/NoPadding";

    private final SecretProperties secretProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyCryptoUtils(SecretProperties secretProperties) {
        this.secretProperties = secretProperties;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "API key cannot be blank");
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to encrypt API key");
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "API key is not configured");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedText);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to decrypt API key");
        }
    }

    public String mask(String encryptedText) {
        String apiKey = decrypt(encryptedText);
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, Math.min(3, apiKey.length())) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private SecretKeySpec keySpec() throws Exception {
        String secret = secretProperties.getApiKeyEncryptionKey();
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "codepilot.secret.api-key-encryption-key is required");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}

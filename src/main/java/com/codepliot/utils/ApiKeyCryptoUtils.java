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

/**
 * API密钥加密解密工具类。
 * <p>
 * 基于 AES-GCM 算法对第三方 API 密钥进行加密存储和解密读取，
 * 同时提供密钥脱敏功能，用于在日志或前端安全地展示密钥片段。
 * </p>
 */
@Component
public class ApiKeyCryptoUtils {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final String CIPHER = "AES/GCM/NoPadding";

    private final SecretProperties secretProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 构造方法，注入加密密钥配置属性。
     *
     * @param secretProperties 加密密钥配置属性，其中包含 API 密钥加密所用的密钥
     */
    public ApiKeyCryptoUtils(SecretProperties secretProperties) {
        this.secretProperties = secretProperties;
    }

    /**
     * 加密明文 API 密钥。
     * <p>
     * 使用 AES-GCM 算法加密，随机生成 12 字节 IV，并将 IV 与密文拼接后进行 Base64 编码返回。
     * </p>
     *
     * @param plainText 明文 API 密钥，不能为空或空白
     * @return Base64 编码的加密字符串（格式：IV + 密文）
     * @throws BusinessException 当明文为空或加密失败时抛出
     */
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

    /**
     * 解密加密后的 API 密钥。
     * <p>
     * 对 Base64 编码的加密字符串进行解码，提取 IV 和密文，然后使用 AES-GCM 算法解密还原明文。
     * </p>
     *
     * @param encryptedText Base64 编码的加密字符串，不能为空或空白
     * @return 解密后的明文 API 密钥
     * @throws BusinessException 当密文为空或解密失败时抛出
     */
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

    /**
     * 对加密的 API 密钥进行脱敏处理。
     * <p>
     * 先解密获取明文，然后保留前 3 位和后 4 位字符，中间用 **** 替代。
     * 若密钥长度不超过 8 位，则直接返回 ****。
     * </p>
     *
     * @param encryptedText Base64 编码的加密字符串
     * @return 脱敏后的密钥字符串，例如 "sk-****abcd"
     */
    public String mask(String encryptedText) {
        String apiKey = decrypt(encryptedText);
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, Math.min(3, apiKey.length())) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 根据配置的加密密钥生成 AES 密钥规格。
     * <p>
     * 将配置密钥经 SHA-256 哈希后截取为 256 位 AES 密钥。
     * </p>
     *
     * @return AES 密钥规格对象
     * @throws Exception 当密钥配置缺失或哈希计算失败时抛出
     */
    private SecretKeySpec keySpec() throws Exception {
        String secret = secretProperties.getApiKeyEncryptionKey();
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "codepilot.secret.api-key-encryption-key is required");
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}

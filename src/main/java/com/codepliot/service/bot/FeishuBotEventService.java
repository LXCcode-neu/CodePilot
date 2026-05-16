package com.codepliot.service.bot;

import com.codepliot.config.BotProperties;
import com.codepliot.exception.BusinessException;
import com.codepliot.model.BotIncomingMessage;
import com.codepliot.model.ErrorCode;
import com.codepliot.model.NotificationChannelType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class FeishuBotEventService {

    private final BotProperties botProperties;
    private final ObjectMapper objectMapper;
    private final BotCommandService botCommandService;

    public FeishuBotEventService(BotProperties botProperties,
                                 ObjectMapper objectMapper,
                                 BotCommandService botCommandService) {
        this.botProperties = botProperties;
        this.objectMapper = objectMapper;
        this.botCommandService = botCommandService;
    }

    public JsonNode handleEvent(String rawBody,
                                String timestamp,
                                String nonce,
                                String signature) {
        verifySignature(rawBody, timestamp, nonce, signature);
        JsonNode root = parse(rawBody);
        if (root.hasNonNull("encrypt")) {
            root = parse(decrypt(root.path("encrypt").asText()));
        }
        if ("url_verification".equals(root.path("type").asText())) {
            verifyToken(root.path("token").asText());
            return objectMapper.createObjectNode().put("challenge", root.path("challenge").asText());
        }

        JsonNode header = root.path("header");
        verifyToken(header.path("token").asText());
        if (!"im.message.receive_v1".equals(header.path("event_type").asText())) {
            return objectMapper.createObjectNode().put("code", 0).put("msg", "ignored");
        }
        JsonNode message = root.path("event").path("message");
        if (!"text".equals(message.path("message_type").asText())) {
            return objectMapper.createObjectNode().put("code", 0).put("msg", "ignored");
        }

        botCommandService.handle(new BotIncomingMessage(
                NotificationChannelType.FEISHU,
                header.path("event_id").asText(),
                message.path("message_id").asText(),
                message.path("chat_id").asText(),
                root.path("event").path("sender").path("sender_id").path("open_id").asText(),
                extractText(message.path("content").asText())
        ));
        return objectMapper.createObjectNode().put("code", 0).put("msg", "ok");
    }

    private String extractText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        try {
            return objectMapper.readTree(content).path("text").asText("");
        } catch (Exception exception) {
            return content;
        }
    }

    private JsonNode parse(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid Feishu event body");
        }
    }

    private void verifyToken(String token) {
        String expected = botProperties.getFeishu().getVerificationToken();
        if (expected != null && !expected.isBlank() && !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                (token == null ? "" : token).getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid Feishu verification token");
        }
    }

    private void verifySignature(String rawBody, String timestamp, String nonce, String signature) {
        String encryptKey = botProperties.getFeishu().getEncryptKey();
        if (encryptKey == null || encryptKey.isBlank()) {
            return;
        }
        if (isBlank(timestamp) || isBlank(nonce) || isBlank(signature)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Missing Feishu event signature");
        }
        String payload = timestamp + nonce + encryptKey + (rawBody == null ? "" : rawBody);
        String expected = HexFormat.of().formatHex(sha256(payload));
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid Feishu event signature");
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to verify Feishu event signature");
        }
    }

    private String decrypt(String encryptedText) {
        String encryptKey = botProperties.getFeishu().getEncryptKey();
        if (encryptKey == null || encryptKey.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Feishu encrypt key is required for encrypted events");
        }
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[16];
            byte[] data = new byte[encrypted.length - 16];
            System.arraycopy(encrypted, 0, iv, 0, 16);
            System.arraycopy(encrypted, 16, data, 0, data.length);
            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha256(encryptKey), "AES"), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(data);
            int length = decrypted.length;
            while (length > 0 && decrypted[length - 1] > 0 && decrypted[length - 1] <= 16) {
                length--;
            }
            return new String(decrypted, 0, length, StandardCharsets.UTF_8).trim();
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Failed to decrypt Feishu event");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package com.gw.api.util.work;

import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkGitAccessTokenEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public WorkGitAccessTokenEncryptor(@Value("${work.git.encryption-key}") String encryptionKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);

        if (decodedKey.length != 16) {
            throw new IllegalArgumentException("work.git.encryption-key는 Base64 인코딩된 16바이트여야 합니다.");
        }

        this.secretKeySpec = new SecretKeySpec(decodedKey, ALGORITHM);
    }

    public String encrypt(String plainToken) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Git access token 암호화에 실패했습니다.");
        }
    }

    public String decrypt(String encryptedToken) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedToken);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[payload.length - IV_LENGTH];

            buffer.get(iv);
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Git access token 복호화에 실패했습니다.");
        }
    }
}

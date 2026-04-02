package com.gw.share.common.util;

import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OtpSecretEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpSecretEncryptor(@Value("${otp.encryption-key}") String encryptionKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encryptionKey);

        if (decodedKey.length != 16) {
            throw new IllegalArgumentException("otp.encryption-key는 Base64 인코딩된 16바이트여야 합니다.");
        }

        this.secretKeySpec = new SecretKeySpec(decodedKey, ALGORITHM);
    }

    // OTP 시크릿을 AES-GCM으로 암호화한다.
    public String encrypt(String plainSecret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainSecret.getBytes());
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OTP 시크릿 암호화에 실패했습니다.");
        }
    }

    // 저장된 OTP 시크릿을 복호화한다.
    public String decrypt(String encryptedSecret) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedSecret);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[payload.length - IV_LENGTH];

            buffer.get(iv);
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OTP 시크릿 복호화에 실패했습니다.");
        }
    }
}

package com.gw.share.common.util;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.gw.share.common.exception.BusinessException;
import com.gw.share.common.exception.ErrorCode;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class OtpTotpUtil {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String ISSUER = "GlassWorld";
    private static final int SECRET_BYTE_LENGTH = 20;
    private static final int OTP_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();
    private final TimeBasedOneTimePasswordGenerator generator =
            new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), OTP_LENGTH);

    // Google Authenticator 호환 Base32 시크릿을 생성한다.
    public String generateSecret() {
        byte[] randomBytes = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return encodeBase32(randomBytes);
    }

    // Google Authenticator가 읽을 otpauth URL을 생성한다.
    public String buildOtpAuthUrl(String loginId, String secret) {
        String label = URLEncoder.encode(ISSUER + ":" + loginId, StandardCharsets.UTF_8);
        String encodedIssuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + encodedIssuer;
    }

    // 현재 코드와 인접 윈도우를 허용해 OTP를 검증한다.
    public boolean verify(String secret, String code) {
        byte[] decodedSecret = decodeBase32(secret);
        SecretKeySpec secretKey = new SecretKeySpec(decodedSecret, generator.getAlgorithm());
        Instant now = Instant.now();

        for (long offset = -1; offset <= 1; offset++) {
            try {
                int generatedCode = generator.generateOneTimePassword(secretKey, now.plusSeconds(offset * 30));
                if (String.format(Locale.ROOT, "%06d", generatedCode).equals(code)) {
                    return true;
                }
            } catch (InvalidKeyException exception) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "OTP 코드 생성에 실패했습니다.");
            }
        }

        return false;
    }

    private String encodeBase32(byte[] input) {
        StringBuilder builder = new StringBuilder();
        int current = 0;
        int bitsRemaining = 0;

        for (byte value : input) {
            current = (current << 8) | (value & 0xFF);
            bitsRemaining += 8;

            while (bitsRemaining >= 5) {
                int index = (current >> (bitsRemaining - 5)) & 0x1F;
                builder.append(BASE32_ALPHABET.charAt(index));
                bitsRemaining -= 5;
            }
        }

        if (bitsRemaining > 0) {
            int index = (current << (5 - bitsRemaining)) & 0x1F;
            builder.append(BASE32_ALPHABET.charAt(index));
        }

        return builder.toString();
    }

    private byte[] decodeBase32(String input) {
        String normalized = input.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        ByteBuffer buffer = ByteBuffer.allocate((normalized.length() * 5) / 8);
        int current = 0;
        int bitsRemaining = 0;

        for (int index = 0; index < normalized.length(); index++) {
            int alphabetIndex = BASE32_ALPHABET.indexOf(normalized.charAt(index));

            if (alphabetIndex < 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "유효하지 않은 OTP 시크릿입니다.");
            }

            current = (current << 5) | alphabetIndex;
            bitsRemaining += 5;

            if (bitsRemaining >= 8) {
                buffer.put((byte) ((current >> (bitsRemaining - 8)) & 0xFF));
                bitsRemaining -= 8;
            }
        }

        return buffer.array();
    }
}

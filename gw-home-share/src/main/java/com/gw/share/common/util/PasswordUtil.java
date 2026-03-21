package com.gw.share.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordUtil {

    private static final BCryptPasswordEncoder BCRYPT_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    public static String encodeWithBcrypt(String rawPassword) {
        return BCRYPT_PASSWORD_ENCODER.encode(rawPassword);
    }

    public static boolean matchesBcrypt(String rawPassword, String encodedPassword) {
        return BCRYPT_PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }
}

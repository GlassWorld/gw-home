package com.gw.share.common.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    private static final String INIT_ADMIN_PASSWORD_HASH = "$2a$10$okcY7.JTQKdPlB5TKyEipuKjRL6gepDk5h7caUU./mvql0sT.oExS";

    @Test
    void encodeWithBcryptCreatesMatchingHash() {
        String encodedPassword = PasswordUtil.encodeWithBcrypt("admin!@34");

        assertNotEquals("admin!@34", encodedPassword);
        assertTrue(PasswordUtil.matchesBcrypt("admin!@34", encodedPassword));
        assertFalse(PasswordUtil.matchesBcrypt("wrong-password", encodedPassword));
    }

    @Test
    void initAdminPasswordHashMatchesExpectedPassword() {
        assertTrue(PasswordUtil.matchesBcrypt("admin!@34", INIT_ADMIN_PASSWORD_HASH));
        assertFalse(PasswordUtil.matchesBcrypt("Admin1234!", INIT_ADMIN_PASSWORD_HASH));
    }
}

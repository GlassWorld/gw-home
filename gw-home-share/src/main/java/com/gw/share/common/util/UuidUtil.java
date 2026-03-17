package com.gw.share.common.util;

import java.util.UUID;

public final class UuidUtil {

    private UuidUtil() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static boolean isValid(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

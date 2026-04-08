package com.gw.share.common.policy;

public final class BoardSharePolicy {

    public static final int DEFAULT_EXPIRATION_DAYS = 7;
    public static final int MAX_EXPIRATION_DAYS = 30;
    public static final int MIN_PASSWORD_LENGTH = 4;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final long EXPIRING_SOON_HOURS = 24L;

    public static final String SHARE_STATUS_INACTIVE = "INACTIVE";
    public static final String SHARE_STATUS_SHARING = "SHARING";
    public static final String SHARE_STATUS_EXPIRING_SOON = "EXPIRING_SOON";
    public static final String SHARE_STATUS_EXPIRED = "EXPIRED";
    public static final String SHARE_STATUS_REVOKED = "REVOKED";

    public static final String ACCESS_STATUS_AVAILABLE = "AVAILABLE";
    public static final String ACCESS_STATUS_PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    public static final String ACCESS_STATUS_EXPIRED = "EXPIRED";
    public static final String ACCESS_STATUS_REVOKED = "REVOKED";
    public static final String ACCESS_STATUS_NOT_FOUND = "NOT_FOUND";
    public static final String ACCESS_STATUS_UNAVAILABLE = "UNAVAILABLE";

    private BoardSharePolicy() {
    }
}

package com.gw.share.common.policy;

public final class AuthPolicy {

    public static final int MAX_LOGIN_FAIL_COUNT = 5;
    public static final int MAX_OTP_FAIL_COUNT = 5;
    public static final int OTP_LOCK_MINUTES = 30;
    public static final String LOGIN_STATUS_OTP_REQUIRED = "OTP_REQUIRED";
    public static final String LOGIN_STATUS_OTP_SETUP_REQUIRED = "OTP_SETUP_REQUIRED";
    public static final String LOGIN_STATUS_SUCCESS = "SUCCESS";
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    private AuthPolicy() {
    }
}

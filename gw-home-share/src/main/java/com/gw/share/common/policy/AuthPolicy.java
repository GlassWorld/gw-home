package com.gw.share.common.policy;

public final class AuthPolicy {

    public static final int MAX_LOGIN_FAIL_COUNT = 5;
    public static final int MAX_OTP_FAIL_COUNT = 5;
    public static final int OTP_LOCK_MINUTES = 30;

    private AuthPolicy() {
    }
}

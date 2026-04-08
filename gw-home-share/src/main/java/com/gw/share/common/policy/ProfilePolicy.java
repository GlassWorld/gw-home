package com.gw.share.common.policy;

import java.util.Set;

public final class ProfilePolicy {

    public static final int MAX_FAVORITE_MENU_COUNT = 5;
    public static final Set<String> USER_NAVIGATION_MENU_PATHS = Set.of(
            "/dashboard",
            "/notices",
            "/board",
            "/weekly-reports",
            "/work",
            "/work/todos",
            "/work/git-accounts",
            "/work/daily-reports",
            "/work/weekly-reports",
            "/vault",
            "/settings",
            "/security"
    );
    public static final Set<String> ADMIN_NAVIGATION_MENU_PATHS = Set.of(
            "/admin/accounts",
            "/admin/board-categories",
            "/admin/daily-reports",
            "/admin/notices",
            "/admin/vault-categories"
    );

    private ProfilePolicy() {
    }
}

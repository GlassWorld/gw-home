package com.gw.share.common.query;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value, SortDirection defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        for (SortDirection direction : values()) {
            if (direction.name().equalsIgnoreCase(value)) {
                return direction;
            }
        }

        return defaultValue;
    }
}

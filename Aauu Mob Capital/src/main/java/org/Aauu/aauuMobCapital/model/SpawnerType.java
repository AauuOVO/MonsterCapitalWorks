package org.Aauu.aauuMobCapital.model;

/**
 * 刷怪笼类型枚举
 */
public enum SpawnerType {
    NORMAL("normal", "普通刷怪笼"),
    PREMIUM("premium", "付费刷怪笼");

    private final String key;
    private final String displayName;

    SpawnerType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SpawnerType fromKey(String key) {
        for (SpawnerType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return NORMAL;
    }
}

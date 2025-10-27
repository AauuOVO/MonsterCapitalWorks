package org.mcw.monstercapitalworks.model

/**
 * 刷怪笼类型枚举
 */
enum class SpawnerType(val key: String) {
    NORMAL("normal"),
    PREMIUM("premium");

    companion object {
        fun fromKey(key: String): SpawnerType? = values().find { it.key == key }
    }
}

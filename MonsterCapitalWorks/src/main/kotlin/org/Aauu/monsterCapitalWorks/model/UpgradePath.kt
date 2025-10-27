package org.Aauu.monsterCapitalWorks.model

data class UpgradePath(
    val name: String,
    val displayName: String,
    val maxLevel: Int,
    val costs: List<Double>,
    val values: List<Any>,
    val requiredUpgrades: Map<Int, Map<String, Int>> = emptyMap()
) {
    fun getCost(level: Int): Double {
        return if (level > 0 && level <= costs.size) {
            costs[level - 1]
        } else {
            0.0
        }
    }

    fun getValue(level: Int): Any? {
        return if (level > 0 && level <= values.size) {
            values[level - 1]
        } else {
            null
        }
    }
}

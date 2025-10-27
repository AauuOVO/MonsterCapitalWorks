package org.mcw.monstercapitalworks.model

/**
 * 升级路径模型
 */
class UpgradePath(
    val key: String,
    var name: String = key,
    var description: String = "",
    var maxLevel: Int = 0,
    private val levels: MutableMap<Int, UpgradeLevel> = mutableMapOf()
) {
    fun getLevels(): Map<Int, UpgradeLevel> = levels.toMap()

    fun getLevel(level: Int): UpgradeLevel? = levels[level]

    fun addLevel(level: Int, upgradeLevel: UpgradeLevel) {
        levels[level] = upgradeLevel
        // 更新最大等级
        if (level > maxLevel) {
            maxLevel = level
        }
    }

    /**
     * 添加等级（简化版本）
     */
    fun addLevel(level: Int, value: Double, cost: Double) {
        val upgradeLevel = UpgradeLevel(cost.toInt(), value)
        addLevel(level, upgradeLevel)
    }

    /**
     * 检查是否有指定等级
     */
    fun hasLevel(level: Int): Boolean = levels.containsKey(level)

    /**
     * 获取指定等级的费用
     */
    fun getCost(level: Int): Double = levels[level]?.cost?.toDouble() ?: 0.0

    /**
     * 获取指定等级的值
     */
    fun getValue(level: Int): Double = levels[level]?.value ?: 0.0

    /**
     * 升级等级数据
     */
    data class UpgradeLevel(
        val cost: Int,
        val value: Double,
        private val requiredUpgrades: MutableMap<String, Int> = mutableMapOf()
    ) {
        fun getRequiredUpgrades(): Map<String, Int> = requiredUpgrades.toMap()

        fun addRequiredUpgrade(upgradeKey: String, level: Int) {
            requiredUpgrades[upgradeKey] = level
        }
    }
}

# MonsterCapitalWorks

<div align="center">

![Version](https://img.shields.io/badge/version-2.1-blue.svg)
![Minecraft](https://img.shields.io/badge/minecraft-1.21-green.svg)
![License](https://img.shields.io/badge/license-MIT-yellow.svg)

**一个功能强大的 Minecraft 刷怪笼管理系统插件**

[特性](#-特性) • [安装](#-安装) • [命令](#-命令) • [权限](#-权限) • [配置](#-配置)

</div>

---

## 📖 简介

MonsterCapitalWorks (MCW) 是一个为 Minecraft 服务器设计的高级刷怪笼管理插件，提供完整的刷怪笼控制、升级系统、经济集成和权限管理功能。

## ✨ 特性

### 🎯 核心功能
- **双类型系统** - 支持普通(NORMAL)和付费(PREMIUM)两种刷怪笼类型
- **图形界面** - 完整的 GUI 管理系统，操作简单直观
- **升级系统** - 5种升级路径：速度、数量、范围、最大附近实体、存储容量
- **精确生成** - 支持精确位置生成和随机生成两种模式
- **离线存储** - 玩家离线时自动存储生成，上线后释放

### 💰 经济系统
- **Vault 集成** - 完整的经济系统支持
- **实体解锁** - 付费解锁特定生物类型
- **升级购买** - 使用游戏货币升级刷怪笼属性
- **限制购买** - 购买额外的刷怪笼放置位置

### 🔐 权限系统
- **细粒度控制** - 详细的权限节点配置
- **权限组奖励** - 通过权限组获得额外刷怪笼位置
- **绕过权限** - 管理员可绕过各种限制
- **性能优化** - 使用二分查找优化权限检查

### 🎨 自定义功能
- **RGB 颜色** - 支持 `{#RRGGBB}` 格式的 RGB 颜色代码
- **自定义名称** - 刷怪笼和刷怪蛋的自定义显示名称
- **PlaceholderAPI** - 完整的 PAPI 占位符支持
- **条件系统** - 基于 Y 坐标、生物群系等的生成条件

## 📦 依赖

### 必需
- **Vault** - 经济系统支持

### 可选
- **PlaceholderAPI** - 占位符支持

## 🚀 安装

1. 下载最新版本的插件 jar 文件
2. 将文件放入服务器的 `plugins` 文件夹
3. 确保已安装 Vault 插件
4. 重启服务器
5. 配置文件将自动生成在 `plugins/MonsterCapitalWorks/` 目录

## 📝 命令

### 基础命令
```
/mcw help                              - 显示帮助信息
/mcw reload                            - 重载配置文件
/mcw give <玩家> <类型> [实体]         - 给予刷怪笼
/mcw info                              - 查看刷怪笼信息
/mcw list [玩家]                       - 列出刷怪笼详情
/mcw remove                            - 移除刷怪笼
```

### 管理员命令
```
/mcw limit set <玩家> <类型> <数量>    - 设置玩家额外限制
/mcw limit add <玩家> <类型> <数量>    - 增加玩家额外限制
/mcw limit remove <玩家> <类型> <数量> - 减少玩家额外限制
```

## 🔑 权限

### 基础权限
| 权限节点 | 说明 | 默认值 |
|---------|------|--------|
| `mcw.create.normal` | 创建普通刷怪笼 | true |
| `mcw.create.premium` | 创建付费刷怪笼 | false |
| `mcw.place.normal` | 放置普通刷怪笼 | true |
| `mcw.place.premium` | 放置付费刷怪笼 | false |

### GUI 权限
| 权限节点 | 说明 | 默认值 |
|---------|------|--------|
| `mcw.gui.normal` | 打开普通刷怪笼GUI | true |
| `mcw.gui.premium` | 打开付费刷怪笼GUI | false |
| `mcw.gui.purchase` | 打开购买界面 | true |

### 绕过权限
| 权限节点 | 说明 | 默认值 |
|---------|------|--------|
| `mcw.bypass.conditions` | 绕过生物生成条件 | op |
| `mcw.bypass.disabled` | 绕过生物禁用限制 | op |
| `mcw.bypass.limits` | 绕过所有数量限制 | op |

### 特殊权限
| 权限节点 | 说明 |
|---------|------|
| `mcw.spawnmode.precise` | 使用精确生成模式 |
| `mcw.limit.normal.extra.<数量>` | 普通刷怪笼额外位置 |
| `mcw.limit.premium.extra.<数量>` | 付费刷怪笼额外位置 |

## ⚙️ 配置

### 主要配置文件

- **config.yml** - 主配置文件
  - 数据库设置
  - 经济系统配置
  - 刷怪机制参数
  - 自定义名称配置
  - 性能优化选项

- **limits.yml** - 限制配置
  - 基础限制设置
  - 购买系统配置
  - 价格模式设置

- **messages.yml** - 消息配置
  - 所有插件消息的自定义
  - 支持列表格式消息
  - RGB 颜色支持

### 实体配置
- `entities_normal.yml` - 普通刷怪笼可用实体
- `entities_premium.yml` - 付费刷怪笼可用实体

### 升级配置
- `normal_upgrades.yml` - 普通刷怪笼升级路径
- `premium_upgrades.yml` - 付费刷怪笼升级路径

### GUI 配置
- `gui/main_menu_normal.yml` - 普通刷怪笼主菜单
- `gui/main_menu_premium.yml` - 付费刷怪笼主菜单
- `gui/upgrade_menu.yml` - 升级菜单
- `gui/entity_menu.yml` - 实体选择菜单
- `gui/buy_limit_menu.yml` - 购买限制菜单
- `gui/precise_pos_menu.yml` - 精确位置设置菜单

## 🎮 使用方法

### 放置刷怪笼
1. 使用 `/mcw give` 命令获取刷怪笼
2. 右键放置刷怪笼
3. 右键点击刷怪笼打开 GUI

### 管理刷怪笼
- **右键点击** - 打开 GUI 管理界面
- **Shift+左键** - 拾取刷怪笼（保留升级）

### 升级刷怪笼
1. 打开刷怪笼 GUI
2. 点击"升级"按钮
3. 选择要升级的属性
4. 确认购买

## 🔧 性能优化

### 已实现的优化
- ✅ 权限检查使用二分查找算法（从 O(n) 优化到 O(log n)）
- ✅ 异步数据库操作
- ✅ 刷怪笼处理批量化
- ✅ 配置文件缓存
- ✅ 事件监听器优化

### 配置建议
```yaml
performance:
  spawner_tick_interval: 20      # 刷怪笼处理间隔
  max_spawners_per_tick: 10      # 每tick最大处理数量
  async_processing: true          # 启用异步处理
```

## 📊 占位符

### 基础信息
- `%amc_type%` - 刷怪笼类型
- `%amc_entity%` - 当前实体类型
- `%amc_location%` - 刷怪笼位置
- `%amc_active%` - 激活状态
- `%amc_status%` - 开关状态

### 升级信息
- `%amc_speed_level%` - 速度升级等级
- `%amc_count_level%` - 数量升级等级
- `%amc_range_level%` - 范围升级等级
- `%amc_storage_level%` - 存储升级等级

### 玩家数据
- `%amc_placed%` - 已放置数量
- `%amc_limit%` - 总限制
- `%amc_purchased%` - 已购买额外位置

## 🐛 故障排除

### 常见问题

**Q: 刷怪笼不生成怪物？**
- 检查刷怪笼是否激活
- 确认玩家在激活范围内
- 验证是否满足生成条件
- 检查附近实体数量

**Q: 无法放置刷怪笼？**
- 确认有放置权限
- 检查是否达到数量限制
- 验证是否满足放置条件

**Q: 升级失败？**
- 确认有足够的金钱
- 检查前置升级要求
- 验证是否已达最大等级

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 📞 支持

如有问题或需要帮助：
1. 查看本文档
2. 检查配置文件
3. 启用调试模式查看日志
4. 提交 Issue

## 🔄 更新日志

### v2.1
- ✨ 优化权限检查性能（二分查找算法）
- ✨ 改进消息系统（支持列表格式）
- 🐛 修复生物群系获取方法
- 🐛 修复编译错误
- 📝 完善文档

---

<div align="center">

**感谢使用 MonsterCapitalWorks！**

Made with ❤️ by Aauu

</div>

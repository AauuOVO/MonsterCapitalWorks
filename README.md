# 🎮 MonsterCapitalWorks (MCW)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-brightgreen.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个功能强大、高度可配置的 Minecraft 刷怪笼管理插件，支持经济系统、升级系统、精确生成位置等多种特性。

## ✨ 核心特性

### 🏪 双刷怪笼系统
- **普通刷怪笼 (Normal)** - 基础功能，适合新手玩家
- **付费刷怪笼 (Premium)** - 高级功能，提供更多特性和更高效率

### 💰 经济系统集成
- 完整的 Vault 经济系统支持
- 可配置的实体解锁价格
- 灵活的购买限制系统
- 支持购买额外刷怪笼数量限制
- 两种价格模式：固定价格 / 倍率递增

### 🔧 强大的升级系统
- **生成延迟升级** - 减少刷怪间隔时间，提高效率
- **生成数量升级** - 增加每次生成的实体数量
- **最大附近实体升级** - 提高周围实体上限
- **激活范围升级** - 扩大刷怪笼激活距离
- **存储容量升级** - 增加实体存储上限
- **升级依赖系统** - 支持升级前置条件

### 📦 智能存储系统
- 自动存储生成的实体
- 可配置的最大存储容量
- 一键释放所有存储的实体
- 支持存储功能开关
- 离线也能累积存储

### 🎯 精确生成位置
- **随机模式** - 在刷怪笼周围随机生成
- **精确模式** - 在指定坐标精确生成
- 可视化坐标调整界面
- 支持 X、Y、Z 三轴独立调整（0.1 方块精度）
- 实时预览生成位置

### 🎨 现代化 GUI 界面
- 美观的图形化操作界面
- 直观的实体选择菜单
- 便捷的升级管理界面
- 精确位置设置界面
- 购买限制管理界面
- 完全可自定义的 GUI 布局

### 🎒 便捷拾取系统
- **Shift+左键** 快速拾取刷怪笼
- 自动保留所有升级信息
- 保留精确生成位置设置
- 保留存储的实体数量
- 所有权保护，防止误操作

### 🔐 完善的权限系统
- 细粒度的权限控制
- 支持普通和付费刷怪笼分离权限
- 管理员权限支持
- 绕过限制权限
- 支持权限组数量限制

### 💾 数据持久化
- SQLite 数据库存储
- 完整的刷怪笼状态保存
- 玩家数据自动保存
- 异步数据库操作，不影响性能
- 服务器重启数据不丢失

### 🌍 多语言支持
- 完全可自定义的消息系统
- 支持颜色代码和格式化
- 支持 PlaceholderAPI 变量

## 📋 系统要求

- **Minecraft 版本**: 1.21.4+
- **服务端**: Paper
- **Java 版本**: 21+
- **必需依赖**: 
  - [Vault](https://www.spigotmc.org/resources/vault.34315/)（经济系统）
  - 任意经济插件（如 [EssentialsX](https://essentialsx.net/)）
- **可选依赖**:
  - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)（变量支持）

## 🚀 快速开始

### 安装步骤

1. **下载插件**
   - 自行编译（见下方编译说明）

2. **安装依赖**
   ```
   确保已安装：
   - Vault 插件
   - 经济插件（如 EssentialsX）
   ```

3. **部署插件**
   ```
   将 MonsterCapitalWorks-2.0.jar 放入服务器的 plugins 文件夹
   ```

4. **启动服务器**
   - 首次启动会自动生成配置文件
   - 配置文件位于 `plugins/MonsterCapitalWorks/`

5. **配置插件**（可选）
   - 编辑配置文件以自定义功能
   - 调整实体价格、升级配置等

6. **重载配置**
   ```
   /mcw reload
   ```

### 基础使用

#### 🎁 获取刷怪笼
```
/mcw give <玩家> <类型> [实体]
```
- **类型**: `normal`（普通）或 `premium`（付费）
- **实体**: 可选，默认为 ZOMBIE
- **示例**: `/mcw give Steve normal COW`

#### 🎮 管理刷怪笼
1. **右键点击**刷怪笼打开管理界面
2. 在界面中可以：
   - 切换实体类型
   - 升级各项属性
   - 设置精确生成位置
   - 管理存储系统
   - 购买额外限制

3. **Shift+左键**快速拾取刷怪笼
   - 保留所有升级
   - 保留精确位置设置
   - 保留存储的实体

#### 📊 查看信息
```
/mcw info        # 看向刷怪笼并执行，查看详细信息
/mcw list        # 查看自己的刷怪笼列表
/mcw list <玩家> # 查看其他玩家的刷怪笼（需要权限）
```

## 📖 命令与权限

### 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/mcw` | 显示帮助信息 | 无 |
| `/mcw help` | 显示帮助信息 | 无 |
| `/mcw reload` | 重载配置文件 | `mcw.admin.reload` |
| `/mcw give <玩家> <类型> [实体]` | 给予玩家刷怪笼 | `mcw.admin.give` |
| `/mcw info` | 查看刷怪笼详细信息 | `mcw.admin` |
| `/mcw list [玩家]` | 列出刷怪笼 | `mcw.admin` |
| `/mcw remove` | 移除刷怪笼（不掉落） | `mcw.admin.remove` |

**💡 提示**: 推荐使用 **Shift+左键** 拾取刷怪笼，而不是 `/mcw remove` 命令！

### 权限节点

#### 基础权限
| 权限 | 描述 | 默认 |
|------|------|------|
| `mcw.*` | 所有权限 | op |
| `mcw.create.normal` | 创建普通刷怪笼 | true |
| `mcw.create.premium` | 创建付费刷怪笼 | false |
| `mcw.place.normal` | 放置普通刷怪笼 | true |
| `mcw.place.premium` | 放置付费刷怪笼 | false |

#### GUI 权限
| 权限 | 描述 | 默认 |
|------|------|------|
| `mcw.gui.normal` | 打开普通刷怪笼GUI | true |
| `mcw.gui.premium` | 打开付费刷怪笼GUI | false |
| `mcw.gui.purchase` | 打开购买界面 | true |

#### 管理员权限
| 权限 | 描述 | 默认 |
|------|------|------|
| `mcw.admin` | 所有管理员权限 | op |
| `mcw.admin.reload` | 重载配置 | op |
| `mcw.admin.give` | 给予刷怪笼 | op |
| `mcw.admin.remove` | 移除刷怪笼 | op |
| `mcw.admin.break` | 破坏他人刷怪笼 | op |
| `mcw.admin.use` | 使用他人刷怪笼 | op |
| `mcw.admin.pickup` | 拾取任何玩家的刷怪笼 | op |
| `mcw.admin.list.others` | 查看其他玩家的刷怪笼 | op |

#### 绕过权限
| 权限 | 描述 | 默认 |
|------|------|------|
| `mcw.bypass.conditions` | 绕过生成条件 | op |
| `mcw.bypass.disabled` | 绕过生物禁用限制 | op |
| `mcw.bypass.limits` | 绕过所有数量限制 | op |

#### 特殊权限
| 权限 | 描述 | 默认 |
|------|------|------|
| `mcw.spawnmode.precise` | 使用精确生成模式 | true |

## ⚙️ 配置说明

### 主配置文件 (config.yml)

```yaml
# 数据库设置
database:
  type: sqlite
  file: data/mcw.db

# 经济设置
economy:
  enabled: true

# PlaceholderAPI
placeholderapi:
  enabled: true

# 刷怪笼数量限制
limits:
  normal:
    base: 5                    # 基础限制
    purchase:
      enabled: true            # 是否允许购买
      base_price: 1000.0       # 基础价格
      price_mode: "multiplier" # 价格模式
      price_multiplier: 1.2    # 价格倍率
      max_purchasable: 50      # 最多可购买数量
  
  premium:
    base: 2
    purchase:
      enabled: true
      base_price: 5000.0
      price_mode: "multiplier"
      price_multiplier: 1.5
      max_purchasable: 20

# 刷怪机制设置
spawning:
  default_spawn_count: 1
  default_spawn_delay: 100
  default_max_nearby_entities: 6
  default_activation_range: 16
  
  storage:
    enabled: true
    default_max_storage: 100
    default_storage_time: 3600
    release_interval: 20

# 性能优化
performance:
  spawner_tick_interval: 20
  max_spawners_per_tick: 10
  async_processing: true

# 调试模式
debug: false
```

### 实体配置 (entities_normal.yml / entities_premium.yml)

```yaml
entities:
  ZOMBIE:
    price: 0                   # 免费解锁
    require_unlock: false
    display_name: "§2僵尸"
    
  COW:
    price: 2000                # 需要 2000 金币解锁
    require_unlock: true
    display_name: "§e牛"
    
  BLAZE:
    price: 10000
    require_unlock: true
    display_name: "§6烈焰人"
    conditions:
      - "%world_name% == world_nether"   # PAPI条件：只能在下界生成怪物
    spawn_conditions:
      min_y: 0                           # Y坐标条件：刷怪笼必须放置在Y >= 0
      max_y: 128                         # Y坐标条件：刷怪笼必须放置在Y <= 128
```

### 升级配置 (normal_upgrades.yml / premium_upgrades.yml)

```yaml
upgrades:
  speed:
    name: "&b生成速度"
    description: "&7减少刷怪间隔时间"
    levels:                    # 最大等级由配置的levels数量自动决定
      1:
        cost: 1000
        value: 90              # 延迟降低到 90 tick
      2:
        cost: 1500
        value: 80
      3:
        cost: 2000
        value: 70
  
  count:
    name: "&a生成数量"
    description: "&7增加每波生成的实体数量"
    levels:                    # 配置了5个等级，最大等级就是5
      1:
        cost: 2000
        value: 2
      2:
        cost: 4000
        value: 3
        required_upgrades:     # 需要先升级其他项
          speed: 3             # 需要速度升级到3级
      3:
        cost: 6000
        value: 4
      4:
        cost: 8000
        value: 5
      5:
        cost: 12000
        value: 6
```

**💡 升级系统特性：**
- 最大等级由配置文件中的 `levels` 数量自动决定，无需手动设置 `max_level`
- 达到最大等级后，系统会自动提示"该升级已达到最大等级！无法继续升级。"
- 支持升级前置条件，可以设置某些升级需要其他升级达到指定等级
- 管理员可以随时在配置文件中添加或删除等级，插件会自动适应

### GUI 配置 (gui/*.yml)

所有 GUI 界面都可以完全自定义，包括：
- 界面标题
- 界面大小
- 物品位置
- 物品材质
- 物品名称和描述
- 点击动作

详细配置请参考 `src/main/resources/gui/` 目录下的示例文件。

## 🔨 编译项目

### 前置要求
- JDK 21 或更高版本
- Maven 3.6+
- Git

### 编译步骤

```bash
# 1. 克隆仓库
git clone https://github.com/yourusername/MonsterCapitalWorks.git
cd MonsterCapitalWorks

# 2. 使用 Maven 编译
mvn clean package

# 3. 编译后的文件位于
# target/MonsterCapitalWorks-2.0.jar
```

### 开发环境设置

推荐使用 IntelliJ IDEA：

1. 打开 IntelliJ IDEA
2. File → Open → 选择项目目录
3. 等待 Maven 自动导入依赖
4. 使用 Maven 工具窗口执行 `package` 任务

## 📊 PlaceholderAPI 变量

安装 PlaceholderAPI 后可使用以下变量：

| 变量 | 描述 |
|------|------|
| `%mcw_normal_count%` | 玩家的普通刷怪笼数量 |
| `%mcw_premium_count%` | 玩家的付费刷怪笼数量 |
| `%mcw_normal_limit%` | 玩家的普通刷怪笼限制 |
| `%mcw_premium_limit%` | 玩家的付费刷怪笼限制 |
| `%mcw_normal_purchased%` | 玩家购买的普通限制数量 |
| `%mcw_premium_purchased%` | 玩家购买的付费限制数量 |

## 🐛 问题反馈

遇到问题或有建议？

1. 创建 [Issue](../../issues/new)

**提交 Issue 时请包含：**
- 服务器版本（如 Paper 1.21.4）
- 插件版本
- 完整的错误日志
- 详细的复现步骤
- 相关配置文件

## 🤝 贡献

欢迎贡献代码！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📝 更新日志

### v2.0 (2025-10-13)
- 🎉 完全重写插件架构
- ✨ 新增精确生成位置功能
- ✨ 新增实体存储系统
- ✨ 新增购买限制功能
- 🎨 全新的 GUI 界面设计
- 🔧 升级系统优化
- 🐛 修复多个已知问题
- ⚡ 性能优化

### v1.0 (2025-10-11)
- 🎉 首次发布
- ✨ 基础刷怪笼功能
- 💰 经济系统集成
- 🔧 升级系统

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 作者与贡献者

**主要开发者**
- Aauu - [GitHub](https://github.com/AauuOVO/)

**特别感谢**
- 所有提供反馈和建议的玩家
- 开源社区的支持

## 🔗 相关链接

- [SpigotMC](https://www.spigotmc.org/)（即将发布）

---

<div align="center">

⭐ **如果这个项目对您有帮助，请给个 Star 支持一下！** ⭐

Made with ❤️ by Aauu

</div>

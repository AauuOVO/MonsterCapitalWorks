# 🎮 Aauu Mob Capital - Minecraft刷怪笼插件

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-green.svg)](https://www.minecraft.net/)
[![Spigot](https://img.shields.io/badge/Spigot-API-orange.svg)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-red.svg)](https://www.oracle.com/java/)

一个功能强大、高度可配置的Minecraft刷怪笼管理插件，支持经济系统、升级系统、精确生成位置等多种特性。

## ✨ 核心特性

### 🏪 双刷怪笼系统
- **普通刷怪笼** - 基础功能，适合新手玩家
- **付费刷怪笼** - 高级功能，提供更多特性和更高效率

### 💰 经济系统集成
- 完整的Vault经济系统支持
- 可配置的实体解锁价格
- 灵活的购买限制系统
- 支持购买额外刷怪笼限制

### 🔧 强大的升级系统
- **生成延迟升级** - 减少刷怪间隔时间
- **生成数量升级** - 增加每次生成的实体数量
- **最大附近实体升级** - 提高周围实体上限
- **激活范围升级** - 扩大刷怪笼激活距离
- **存储容量升级** - 增加实体存储上限

### 📦 实体存储系统
- 自动存储生成的实体
- 可配置的最大存储容量
- 一键释放所有存储的实体
- 支持存储功能开关

### 🎯 精确生成位置
- **随机模式** - 在刷怪笼周围随机生成
- **精确模式** - 在指定坐标精确生成
- 可视化坐标调整界面
- 支持X、Y、Z三轴独立调整

### 🎨 现代化GUI界面
- 美观的图形化操作界面
- 直观的实体选择菜单
- 便捷的升级管理界面
- 精确位置设置界面
- 购买限制管理界面

### 🔐 权限系统
- 细粒度的权限控制
- 支持普通和付费刷怪笼分离权限
- 管理员权限支持
- 完整的权限节点文档

### 💾 数据持久化
- SQLite数据库存储
- 完整的刷怪笼状态保存
- 玩家数据自动保存
- 服务器重启数据不丢失

### 🌍 多语言支持
- 完全可自定义的消息系统
- 支持颜色代码和格式化
- 易于翻译和本地化

## 📋 系统要求

- **Minecraft版本**: 1.20+
- **服务端**: Spigot / Paper / Purpur
- **Java版本**: 17+
- **依赖插件**: 
  - Vault (经济系统)
  - 任意经济插件 (如EssentialsX)
- **可选依赖**:
  - PlaceholderAPI (变量支持)

## 🚀 快速开始

### 安装步骤

1. **下载插件**
   ```bash
   # 从Releases页面下载最新版本的jar文件
   ```

2. **安装依赖**
   - 确保已安装Vault插件
   - 确保已安装经济插件（如EssentialsX）

3. **部署插件**
   ```bash
   # 将jar文件放入服务器的plugins文件夹
   /plugins/Aauu_Mob_Capital-1.0.jar
   ```

4. **启动服务器**
   ```bash
   # 启动服务器，插件会自动生成配置文件
   ```

5. **配置插件**
   ```bash
   # 编辑配置文件
   /plugins/AauuMobCapital/config.yml
   /plugins/AauuMobCapital/entities_normal.yml
   /plugins/AauuMobCapital/entities_premium.yml
   ```

6. **重载配置**
   ```bash
   /amc reload
   ```

### 基础使用

1. **获取刷怪笼**
   ```
   /amc give <玩家> <类型> [实体]
   ```
   - 类型：normal（普通）或 premium（付费）
   - 实体：可选，默认为PIG

2. **放置刷怪笼**
   - 将获得的刷怪笼物品放置到地面
   - 刷怪笼会自动开始工作

3. **管理刷怪笼**
   - 右键点击已放置的刷怪笼打开管理界面
   - 可以进行升级、设置精确位置、管理存储等操作

4. **查看刷怪笼信息**
   ```
   /amc info
   ```
   - 看向刷怪笼并执行命令查看详细信息

## 📖 详细文档

### 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/amc` 或 `/amc help` | 显示帮助信息 | `amc.use` |
| `/amc reload` | 重载配置文件 | `amc.admin.reload` |
| `/amc give <玩家> <类型> [实体]` | 给予玩家刷怪笼 | `amc.admin.give` |
| `/amc info` | 查看刷怪笼信息（看向刷怪笼） | `amc.use` |
| `/amc list [玩家]` | 列出玩家的刷怪笼 | `amc.use` / `amc.admin.list.others` |
| `/amc remove` | 移除刷怪笼（看向刷怪笼） | `amc.admin.remove` |

### 权限节点

| 权限 | 描述 | 默认 |
|------|------|------|
| `amc.use` | 使用基础功能 | true |
| `amc.normal` | 使用普通刷怪笼 | true |
| `amc.premium` | 使用付费刷怪笼 | op |
| `amc.admin.reload` | 重载配置 | op |
| `amc.admin.give` | 给予刷怪笼 | op |
| `amc.admin.remove` | 移除刷怪笼 | op |
| `amc.admin.list.others` | 查看其他玩家的刷怪笼 | op |
| `amc.bypass.limit` | 绕过放置限制 | op |
| `amc.bypass.cost` | 绕过购买费用 | op |

### 配置文件说明

#### config.yml - 主配置文件
```yaml
# 数据库配置
database:
  type: sqlite
  
# 刷怪笼限制
spawner_limits:
  normal:
    default: 5        # 默认普通刷怪笼限制
    max_purchasable: 20  # 最多可购买的额外限制
  premium:
    default: 3        # 默认付费刷怪笼限制
    max_purchasable: 15  # 最多可购买的额外限制

# 购买限制价格
buy_limit_prices:
  normal: 10000      # 每个普通刷怪笼限制的价格
  premium: 50000     # 每个付费刷怪笼限制的价格
```

#### entities_normal.yml - 普通刷怪笼实体配置
```yaml
# require_unlock 字段说明：
#   true  = 免费解锁（默认值）
#   false = 需要花钱购买解锁

entities:
  COW:
    price: 2000
    # require_unlock: true  # 默认免费解锁
  
  ZOMBIE:
    price: 500
    require_unlock: false  # 需要花钱解锁
```

#### entities_premium.yml - 付费刷怪笼实体配置
```yaml
entities:
  COW:
    price: 5000
  
  VILLAGER:
    price: 10000
  
  WITHER:
    price: 100000
    enabled: false  # 禁用此实体
```

#### 升级配置文件

**normal_upgrades.yml** - 普通刷怪笼升级
```yaml
upgrades:
  spawn_delay:
    name: "生成延迟"
    max_level: 10
    levels:
      1:
        cost: 5000
        value: 80
```

**premium_upgrades.yml** - 付费刷怪笼升级
```yaml
upgrades:
  spawn_delay:
    name: "生成延迟"
    max_level: 15
    levels:
      1:
        cost: 10000
        value: 70
```

## 🎯 功能展示

### 刷怪笼获取与放置
1. 使用命令获取刷怪笼
   ```
   /amc give <玩家> normal PIG
   /amc give <玩家> premium COW
   ```
2. 将刷怪笼物品放置到地面
3. 刷怪笼自动开始工作

### 刷怪笼管理
1. 右键点击刷怪笼打开GUI界面
2. 可以进行以下操作：
   - 升级各项属性
   - 设置精确生成位置
   - 管理实体存储
   - 切换刷怪笼开关状态

### 升级系统
1. 右键点击刷怪笼打开界面
2. 点击"升级"按钮
3. 选择要升级的属性：
   - 生成延迟
   - 生成数量
   - 最大附近实体
   - 激活范围
   - 存储容量
4. 支付费用完成升级

### 精确位置设置
1. 右键点击刷怪笼
2. 点击"精确位置"按钮
3. 使用界面调整X、Y、Z坐标
4. 切换生成模式（随机/精确）

### 实体存储
1. 右键点击刷怪笼
2. 启用存储功能
3. 实体会自动存储而不是直接生成
4. 点击"释放"按钮释放所有存储的实体

### 查看信息
```bash
# 看向刷怪笼并执行
/amc info

# 查看自己的刷怪笼列表
/amc list

# 查看其他玩家的刷怪笼（需要权限）
/amc list <玩家>
```

## 🔧 开发信息

### 项目结构
```
src/main/java/org/Aauu/aauuMobCapital/
├── AauuMobCapital.java          # 主类
├── command/                      # 命令处理
│   └── AMCCommand.java
├── listener/                     # 事件监听
│   └── SpawnerListener.java
├── manager/                      # 管理器
│   ├── ConfigManager.java       # 配置管理
│   ├── DataManager.java         # 数据管理
│   ├── EconomyManager.java      # 经济管理
│   ├── GUIManager.java          # GUI管理
│   ├── PermissionManager.java   # 权限管理
│   ├── SpawnerManager.java      # 刷怪笼管理
│   └── UpgradeManager.java      # 升级管理
├── model/                        # 数据模型
│   ├── PlayerData.java
│   ├── Spawner.java
│   ├── SpawnerType.java
│   └── UpgradePath.java
└── hook/                         # 插件钩子
    └── PlaceholderAPIHook.java
```

### 技术栈
- **语言**: Java 17
- **构建工具**: Maven
- **数据库**: SQLite
- **API**: Spigot API 1.20.1
- **依赖管理**: Maven Shade Plugin

### 编译项目
```bash
# 克隆仓库
git clone https://github.com/yourusername/Aauu-Mob-Capital.git

# 进入项目目录
cd Aauu-Mob-Capital

# 使用Maven编译
mvn clean package

# 编译后的jar文件位于
target/Aauu_Mob_Capital-1.0.jar
```

## 🐛 问题反馈

如果您遇到任何问题或有功能建议，请：

1. 创建 [新Issue](../../issues/new)

提交Issue时请包含：
- 服务器版本
- 插件版本
- 错误日志
- 复现步骤

## 📝 更新日志

### v1.0.0 (2025-01-13)
- ✨ 初始版本发布
- ✅ 双刷怪笼系统
- ✅ 完整的升级系统
- ✅ 精确生成位置
- ✅ 实体存储功能
- ✅ 现代化GUI界面
- ✅ 完整的数据持久化
- ✅ 权限系统
- ✅ 经济系统集成

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 作者

- **Aauu** - *初始工作* - [GitHub](https://github.com/yourusername)

## 🙏 鸣谢

- Spigot团队提供的优秀API
- Vault插件的经济系统支持
- 所有贡献者和用户的支持

## 📞 联系方式

- **问题反馈**: [GitHub Issues](../../issues)
- **功能建议**: [GitHub Discussions](../../discussions)

---

⭐ 如果这个项目对您有帮助，请给个Star支持一下！

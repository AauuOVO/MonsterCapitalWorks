# MonsterCapitalWorks v3.2

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-blue.svg)](https://kotlinlang.org/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 高级刷怪笼管理系统 - Kotlin优化版

一个功能强大的Minecraft Paper/Bukkit插件，提供完整的刷怪笼管理系统，支持MySQL/SQLite数据库、升级系统、GUI界面、精确生成模式和异步处理。

## ✨ 主要特性

### 🎯 核心功能
- **双类型刷怪笼系统**：普通刷怪笼和高级刷怪笼
- **多实体支持**：支持所有Minecraft生物类型
- **灵活的生成模式**：随机生成和精确位置生成
- **存储系统**：关闭时自动存储生物，开启时释放
- **升级系统**：多种升级路径提升刷怪笼性能

### 💾 数据库支持
- **SQLite**：轻量级本地数据库（默认）
- **MySQL**：高性能远程数据库
- **HikariCP连接池**：优化数据库性能
- **异步处理**：所有数据库操作异步执行

### 🎨 用户界面
- **GUI菜单系统**：直观的图形界面
- **完全可自定义**：所有GUI通过YAML配置
- **多语言支持**：完整的消息系统

### ⚡ 性能优化
- **Kotlin协程**：高效的异步任务处理
- **智能缓存**：减少数据库查询
- **批量处理**：优化刷怪笼tick处理
- **内存管理**：自动清理和资源释放

## 📋 系统要求

- **Minecraft版本**：1.21.4+
- **服务器核心**：Paper / Spigot / Bukkit
- **Java版本**：21+
- **必需依赖**：Vault
- **可选依赖**：PlaceholderAPI, PlayerPoints

## 🚀 快速开始

### 安装步骤

1. **下载插件**
   ```bash
   # 从Releases页面下载最新版本
   wget https://github.com/Aauu/MonsterCapitalWorks/releases/latest/download/MonsterCapitalWorks.jar
   ```

2. **安装依赖**
   - 安装 [Vault](https://www.spigotmc.org/resources/vault.34315/)
   - （可选）安装 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
   - （可选）安装 [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/)

3. **部署插件**
   ```bash
   # 将jar文件放入plugins文件夹
   cp MonsterCapitalWorks.jar /path/to/server/plugins/
   
   # 启动服务器
   java -jar paper.jar
   ```

4. **配置插件**
   - 编辑 `plugins/MonsterCapitalWorks/config.yml`
   - 根据需要配置数据库类型
   - 调整性能参数

### 基础配置

```yaml
# config.yml 示例
database:
  type: sqlite  # 或 mysql
  
economy:
  enabled: true
  
performance:
  spawner_tick_interval: 20
  max_spawners_per_tick: 10
  async_processing: true
```

## 📖 使用指南

### 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/mcw help` | 显示帮助信息 | mcw.use |
| `/mcw give <玩家> <类型> [实体]` | 给予刷怪笼 | mcw.admin.give |
| `/mcw info` | 查看刷怪笼信息 | mcw.use |
| `/mcw list [玩家]` | 列出刷怪笼 | mcw.use |
| `/mcw remove` | 移除刷怪笼 | mcw.admin.remove |
| `/mcw reload` | 重载配置 | mcw.admin.reload |
| `/mcw limit <set\|add\|remove> <玩家> <类型> <数量>` | 管理限制 | mcw.admin.limit |

### 权限节点

```yaml
# 基础权限
mcw.use              # 使用刷怪笼
mcw.use.normal       # 使用普通刷怪笼
mcw.use.premium      # 使用高级刷怪笼
mcw.place            # 放置刷怪笼
mcw.place.normal     # 放置普通刷怪笼
mcw.place.premium    # 放置高级刷怪笼

# 管理员权限
mcw.admin            # 所有管理员权限
mcw.admin.reload     # 重载配置
mcw.admin.give       # 给予刷怪笼
mcw.admin.remove     # 移除刷怪笼

# 特殊权限
mcw.bypass.conditions  # 绕过放置条件
mcw.bypass.limits      # 绕过数量限制
mcw.spawnmode.precise  # 使用精确生成模式
```

## 🔧 高级配置

### 数据库配置

#### SQLite（默认）
```yaml
database:
  type: sqlite
  sqlite:
    file: data/mcw.db
```

#### MySQL
```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: mcw_database
    username: root
    password: password
    pool:
      maximum_pool_size: 10
      minimum_idle: 5
```

### 性能优化

```yaml
performance:
  # 刷怪笼处理间隔（tick）
  spawner_tick_interval: 20
  
  # 每tick最大处理数量
  max_spawners_per_tick: 10
  
  # 异步处理
  async_processing: true
  
  # 存储设置
  storage_add_interval: 20
  storage_release_interval: 100
  storage_release_amount: 1
```

### 升级系统

刷怪笼支持多种升级类型：
- **生成延迟**：减少生成间隔
- **生成数量**：增加每次生成数量
- **激活范围**：扩大激活范围
- **最大实体**：增加附近最大实体数
- **存储容量**：增加存储上限

配置文件：`upgrades/normal_upgrades.yml` 和 `upgrades/premium_upgrades.yml`

## 🏗️ 架构设计

### 技术栈
- **语言**：Kotlin 2.3.0
- **构建工具**：Maven
- **数据库**：SQLite / MySQL + HikariCP
- **异步处理**：Kotlin Coroutines + Custom AsyncTaskManager
- **依赖注入**：自定义DI容器

### 项目结构
```
MonsterCapitalWorks/
├── src/main/kotlin/
│   └── org/Aauu/monsterCapitalWorks/
│       ├── async/          # 异步任务管理
│       ├── cache/          # 缓存系统
│       ├── command/        # 命令系统
│       ├── di/             # 依赖注入
│       ├── hook/           # 第三方插件钩子
│       ├── listener/       # 事件监听器
│       ├── manager/        # 核心管理器
│       ├── model/          # 数据模型
│       └── util/           # 工具类
└── src/main/resources/
    ├── config.yml          # 主配置
    ├── messages.yml        # 消息配置
    ├── spawner/            # 刷怪笼配置
    ├── entities/           # 实体配置
    ├── upgrades/           # 升级配置
    └── gui/                # GUI配置
```

### 核心模块

#### DataManager
- 数据库连接管理
- CRUD操作封装
- 异步数据持久化

#### SpawnerManager
- 刷怪笼生命周期管理
- 生成逻辑处理
- 存储系统

#### AsyncTaskManager
- 异步任务调度
- 线程池管理
- 任务监控

#### CacheManager
- 智能缓存
- 自动过期
- 内存优化

## 🔄 版本历史

### v3.2 (2025-01-28) - Kotlin优化版
- ✅ 移除所有非空断言(!!)，使用安全调用
- ✅ 优化Kotlin代码风格，使用idiomatic Kotlin
- ✅ 将Spawner和PlayerData转换为data class
- ✅ 优化扩展函数，移除重复代码
- ✅ 改进错误处理和空安全
- ✅ 代码文档完善

### v3.1
- 初始Kotlin版本
- 基础功能实现

## 🤝 贡献指南

欢迎贡献！请遵循以下步骤：

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

### 代码规范
- 遵循Kotlin官方代码风格
- 使用有意义的变量和函数名
- 添加适当的注释和文档
- 避免使用非空断言(!!)
- 优先使用Kotlin标准库函数

## 📝 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- [Paper](https://papermc.io/) - 高性能Minecraft服务器
- [Vault](https://github.com/MilkBowl/Vault) - 经济系统API
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - 变量系统
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - 数据库连接池

## 📧 联系方式

- **作者**：Aauu
- **GitHub**：[https://github.com/Aauu/MonsterCapitalWorks](https://github.com/Aauu/MonsterCapitalWorks)
- **问题反馈**：[Issues](https://github.com/Aauu/MonsterCapitalWorks/issues)

---

⭐ 如果这个项目对你有帮助，请给个Star！

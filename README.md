# 🎮 Aauu Mob Capital

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20+-green.svg)](https://www.minecraft.net/)
[![Java](https://img.shields.io/badge/Java-17+-red.svg)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个功能强大、高度可配置的 Minecraft 刷怪笼管理插件，支持经济系统、升级系统、精确生成位置等多种特性。

## ✨ 核心特性

### 🏪 双刷怪笼系统
- **普通刷怪笼** - 基础功能，适合新手玩家
- **付费刷怪笼** - 高级功能，提供更多特性和更高效率

### 💰 经济系统
- 完整的 Vault 经济系统支持
- 可配置的实体解锁价格
- 灵活的购买限制系统
- 支持购买额外刷怪笼限制

### 🔧 升级系统
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
- 支持 X、Y、Z 三轴独立调整

### 🎨 现代化 GUI 界面
- 美观的图形化操作界面
- 直观的实体选择菜单
- 便捷的升级管理界面
- 精确位置设置界面
- 购买限制管理界面

### 🔐 权限系统
- 细粒度的权限控制
- 支持普通和付费刷怪笼分离权限
- 管理员权限支持

### 💾 数据持久化
- SQLite 数据库存储
- 完整的刷怪笼状态保存
- 玩家数据自动保存
- 服务器重启数据不丢失

### 🌍 多语言支持
- 完全可自定义的消息系统
- 支持颜色代码和格式化

## 📋 系统要求

- **Minecraft 版本**: 1.20+
- **服务端**: Spigot / Paper / Purpur
- **Java 版本**: 17+
- **依赖插件**: 
  - Vault（经济系统）
  - 任意经济插件（如 EssentialsX）
- **可选依赖**:
  - PlaceholderAPI（变量支持）

## 🚀 安装与使用

### 安装步骤

1. **下载插件**
   - 从 [Releases](../../releases) 页面下载最新版本的 jar 文件
   - 或者自行编译（见下方编译说明）

2. **安装依赖**
   - 确保已安装 Vault 插件
   - 确保已安装经济插件（如 EssentialsX）

3. **部署插件**
   ```
   将 jar 文件放入服务器的 plugins 文件夹
   ```

4. **启动服务器**
   - 启动服务器，插件会自动生成配置文件

5. **配置插件**
   - 编辑 `plugins/AauuMobCapital/` 目录下的配置文件
   - 根据需要调整实体价格、升级配置等

6. **重载配置**
   ```
   /amc reload
   ```

### 基础使用

#### 获取刷怪笼
```
/amc give <玩家> <类型> [实体]
```
- 类型：`normal`（普通）或 `premium`（付费）
- 实体：可选，默认为 PIG

#### 管理刷怪笼
- 右键点击已放置的刷怪笼打开管理界面
- 可以进行升级、设置精确位置、管理存储等操作

#### 查看信息
```
/amc info        # 看向刷怪笼并执行命令查看详细信息
/amc list        # 查看自己的刷怪笼列表
```

## 📖 命令与权限

### 命令列表

| 命令 | 描述 | 权限 |
|------|------|------|
| `/amc` 或 `/amc help` | 显示帮助信息 | `amc.use` |
| `/amc reload` | 重载配置文件 | `amc.admin.reload` |
| `/amc give <玩家> <类型> [实体]` | 给予玩家刷怪笼 | `amc.admin.give` |
| `/amc info` | 查看刷怪笼信息 | `amc.use` |
| `/amc list [玩家]` | 列出刷怪笼 | `amc.use` |
| `/amc remove` | 移除刷怪笼 | `amc.admin.remove` |

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

## ⚙️ 配置说明

### 主配置文件 (config.yml)

```yaml
# 刷怪笼限制
spawner_limits:
  normal:
    default: 5              # 默认普通刷怪笼限制
    max_purchasable: 20     # 最多可购买的额外限制
  premium:
    default: 3              # 默认付费刷怪笼限制
    max_purchasable: 15     # 最多可购买的额外限制

# 购买限制价格
buy_limit_prices:
  normal: 10000             # 每个普通刷怪笼限制的价格
  premium: 50000            # 每个付费刷怪笼限制的价格
```

### 实体配置 (entities_normal.yml / entities_premium.yml)

```yaml
entities:
  COW:
    price: 2000
    # require_unlock: true   # 默认免费解锁
  
  ZOMBIE:
    price: 500
    require_unlock: false    # 需要花钱解锁
```

### 升级配置 (normal_upgrades.yml / premium_upgrades.yml)

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

## 🔨 编译项目

### 前置要求
- JDK 17 或更高版本
- Maven 3.6+

### 编译步骤

```bash
# 克隆仓库
git clone https://github.com/yourusername/Aauu-Mob-Capital.git

# 进入项目目录
cd Aauu-Mob-Capital

# 使用 Maven 编译
mvn clean package

# 编译后的 jar 文件位于
# target/Aauu_Mob_Capital-1.0.jar
```

## 🐛 问题反馈

如果您遇到任何问题或有功能建议，请：

1. 创建 [Issue](../../issues/new)
2. 提供详细的错误信息和复现步骤

提交 Issue 时请包含：
- 服务器版本
- 插件版本
- 错误日志
- 复现步骤

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 👥 作者

**Aauu** - [GitHub](https://github.com/AauuOVO/)

---

⭐ 如果这个项目对您有帮助，请给个 Star 支持一下！

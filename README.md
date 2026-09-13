# 青账

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 开发语言 | Kotlin 2.0.21 |
| UI | Android XML、ViewBinding、Material Components |
| 架构模式 | MVVM、Repository、模块化架构 |
| 页面导航 | AndroidX Navigation、ARouter |
| 异步与状态管理 | Kotlin Coroutines、Flow、StateFlow |
| 本地数据库 | Room 2.6.1 |
| 轻量数据存储 | MMKV 1.3.3 |
| 列表组件 | RecyclerView、BaseRecyclerViewAdapterHelper |
| 数据可视化 | MPAndroidChart、自定义图表组件 |
| 图片加载 | Coil 2.6.0 |
| 构建工具 | Gradle 8.13、Android Gradle Plugin 8.13.2 |
| 依赖管理 | Gradle Version Catalog |
| 测试 | JUnit、AndroidX Test、Espresso |

### Android 配置

| 配置项 | 当前值 |
| --- | --- |
| Application ID | `com.example.smartwallet` |
| Compile SDK | 36 |
| Target SDK | 36 |
| Min SDK | 26 |
| JVM Target | 11 |

## 项目架构

项目采用多模块架构，将应用组装、业务功能、公共能力和 UI 资源分离。

```text
SmartWallet
├── app
│   ├── 应用入口
│   ├── 启动页
│   ├── 主页面导航
│   └── 业务模块组装
│
├── business
│   ├── home
│   │   ├── api
│   │   └── impl
│   ├── bill
│   │   ├── api
│   │   └── impl
│   ├── statistics
│   │   ├── api
│   │   └── impl
│   ├── budget
│   │   ├── api
│   │   └── impl
│   └── profile
│       ├── api
│       └── impl
│
├── foundation
│   ├── common
│   ├── network
│   ├── storage
│   ├── uikit
│   └── webview
│
├── design
│   └── UI 设计资源
│
└── gradle
    ├── Version Catalog
    └── Gradle Wrapper
```

### 模块职责

| 模块 | 职责 |
| --- | --- |
| `app` | 应用入口、Activity、主导航以及业务实现模块组装 |
| `business:home` | 首页财务概览、近期账单和趋势展示 |
| `business:bill` | 账单记录、查询、编辑、删除及分类数据 |
| `business:statistics` | 收支汇总、分类占比和趋势统计 |
| `business:budget` | 月度预算、分类预算和预算使用情况 |
| `business:profile` | 用户状态、个人资料及设置页面 |
| `foundation:common` | Base 类、公共工具、路由常量和布局预加载 |
| `foundation:network` | 网络基础能力封装 |
| `foundation:storage` | Room 数据库工厂与 MMKV 存储封装 |
| `foundation:uikit` | 通用 UI 与图表组件 |
| `foundation:webview` | WebView 基础能力 |

### API/Impl 分层

每个主要业务模块由 `api` 和 `impl` 两部分组成：

```text
business:feature:api
├── 对外服务接口
├── 路由地址
└── 公共数据模型

business:feature:impl
├── Activity / Fragment
├── ViewModel
├── Repository
├── 数据源实现
└── 页面资源
```

- `api` 模块用于声明跨模块可见的契约，保持轻量和稳定。
- `impl` 模块负责具体业务逻辑、数据处理和界面实现。
- 其他业务模块只能依赖目标模块的 `api`，不能直接依赖其 `impl`。
- `app` 模块负责引入并组装所有业务实现。

### 依赖规则

```text
app
├── business:*:impl
└── foundation:common

business:*:impl
├── 当前业务的 api
├── 其他业务的 api
└── foundation 子模块

business:*:api
└── 轻量公共依赖

foundation
└── 不依赖 business
```

根构建脚本会检查模块依赖关系：

1. `foundation` 模块不能依赖任何 `business` 模块。
2. 不同业务模块之间只能通过对方的 `api` 通信。
3. 业务模块不能直接依赖其他业务模块的 `impl`。

发现非法依赖时，Gradle 会直接终止构建。

### MVVM 数据流

```text
Activity / Fragment
        │
        │ 用户操作、观察 UI State
        ▼
     ViewModel
        │
        │ 调用业务接口
        ▼
DataService / Repository
        │
        │ 访问或更新数据
        ▼
 Room / MMKV / DataSource
        │
        │ Flow 数据变化
        └──────────────► ViewModel ──────────────► UI
```

- Activity 和 Fragment 只负责界面展示与用户交互。
- ViewModel 负责页面状态、数据组合和生命周期内的异步任务。
- DataService 为跨模块业务能力提供统一入口。
- Repository 负责协调具体数据源并隐藏存储实现。
- Flow 和 StateFlow 将数据变化响应式地传递到界面。

### 页面导航

项目组合使用两种导航方式：

- AndroidX Navigation：负责主 Activity 内 Fragment 页面切换及 Deep Link。
- ARouter：负责跨模块 Activity 跳转和业务 Service 查找。

底部主导航包含：首页、账单、统计和我的；中间的独立操作按钮进入添加账单页面。

### 数据层

```text
业务模块
   │
   ├── Room
   │   ├── Entity
   │   ├── DAO
   │   └── Database
   │
   └── MMKV
       └── 轻量配置与状态数据
```

- Room 用于保存结构化业务数据，并通过 DAO 和 Flow 提供响应式查询。
- MMKV 用于保存预算、登录标记、通知设置等轻量键值数据。
- 不同用户的数据通过用户 ID 进行隔离。

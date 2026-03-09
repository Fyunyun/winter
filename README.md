# 项目经历 — WinterServer 多人在线游戏服务器

---

## 项目简介

| | |
|:---|:---|
| **项目名称** | WinterServer — 多人在线游戏服务器 |
| **项目类型** | 个人独立项目（全栈开发） |
| **项目规模** | Java 源文件 73+、Protobuf IDL 13 个、业务模块 10 个、战斗技能配置 30+ 种 |
| **技术栈** | Java 17 / Netty 4.1 / Protobuf 3.21 / Redis (Jedis) / MySQL 8.0 / HikariCP / Spring Context 6.1 / FastJSON2 / Logback / Maven |
| **源码地址** | [GitHub](#) |

---

## 项目描述

独立设计并实现了一套完整的多人在线游戏服务端，基于 **Netty + Protobuf** 构建高性能 TCP 长连接通信，采用 **Redis + MySQL** 双层存储架构。系统涵盖 **10 个业务模块**（登录注册、建筑经营、资源采集、移动与 AOI、实时聊天、好友社交、回合制战斗等），包含一套**高度数据驱动的回合制战斗引擎**（25+ 个类文件，运用策略/工厂/模板方法/组合 4 种设计模式），并开发了 1900+ 行的 HTML5 Canvas 前端战斗可视化联调页。

---

## 核心职责与技术实现

### 一、网络通信架构

- 基于 **Netty NIO** 搭建 TCP 游戏服务器，设计 Netty Pipeline 编排：`IdleStateHandler`（心跳检测）→ `ProtobufVarint32FrameDecoder`（粘包/半包解决）→ `ProtobufDecoder` → `AuthenticationHandler`（网关级鉴权）→ `ServerHandler`（业务入口）
- 设计 **二层信封协议**（`GamePacket` 外层 + 内层业务 Proto），定义 31 个 CmdId 消息号，覆盖 10 个业务模块的请求/响应/广播
- 实现自定义 **`@GameHandler` 注解 + `MessageDispatcher` 反射路由器**，启动时扫描注册，运行时 O(1) 分发消息，类似 Spring MVC `@RequestMapping` 但针对 TCP 协议优化
- Pipeline 层实现 **`AuthenticationHandler` 网关鉴权**，登录/注册请求直接放行，其他请求检查 Session 状态，鉴权逻辑与业务逻辑完全解耦

### 二、存储与缓存设计

- 设计 **Redis + MySQL 双层读写策略**：读请求优先查 Redis Hash，未命中回查 MySQL 并回填 Redis（TTL 3600s）；写请求先更新内存 `PlayerModel` + 同步写 Redis，通过 `isDirty` 脏标记 + `ScheduledExecutorService` 每 5 分钟批量落库 MySQL
- 实现 **双重数据落库保障**：定时 5 分钟全服批量刷盘 + `channelInactive` 断线即时落库，防止宕机/掉线导致数据丢失
- 使用 **HikariCP** 连接池管理 MySQL 连接，**JedisPool** 管理 Redis 连接，`DbManager` 统一封装双连接池生命周期

### 三、AOI（视野兴趣区域）系统

- 利用 **Redis GEO** 原生命令实现轻量 AOI：`GEOADD` 更新坐标、`GEORADIUS` 范围查询 500 单位内的玩家、`ZREM` 下线清理
- 玩家移动时异步更新 Redis 坐标（`CompletableFuture` 不阻塞 Netty I/O 线程），查询视野内邻居后通过 **4 线程固定线程池异步广播**位置变更

### 四、聊天与敏感词过滤

- 支持私聊与世界广播两种模式，目标不在线时消息序列化为 Proto 字节存入 **Redis List**（7 天 TTL），登录时批量推送离线消息
- 手写实现 **Aho-Corasick（AC 自动机）多模式匹配算法**：Trie 树构建 + BFS 失败指针 + 输出链接合并，O(n + m + z) 时间复杂度，支持重叠匹配、批量词库从 MySQL 动态加载，聊天时自动替换敏感词为 `***`

### 五、回合制战斗引擎（项目核心亮点 ★）

- 独立设计并实现了 **25+ 个类文件**的数据驱动战斗系统，划分为 config（配置层）/ core（引擎核心）/ model（战斗模型）三层架构
- **策略模式**：定义 `BattleEffect` 接口，实现 5 种可插拔效果（伤害修正 / 吸血 / 治疗 / 施加 Buff / 冷却缩减），新增效果类型无需修改引擎代码
- **工厂模式**：`EffectFactory` 注册表式创建效果实例、`SkillFactory` 将 JSON 配置翻译为 `GeneralSkill` 运行时对象
- **模板方法**：`AbstractBattleSkill.canTrigger()` 统一五层过滤链（触发时机 → 控制状态 → 冷却 CD → 概率判定 → 特殊条件），子类通过 `checkSpecificConditions()` 扩展
- **组合模式**：一个技能 = N 个条件（AND 组合，11 种条件类型）+ M 个效果（6 种效果类型），策划通过 JSON 配置表零代码新增技能
- 实现完整的 **Buff 系统**（5 大分类：属性修改 / 持续伤害 / 持续回复 / 护盾 / 控制），支持叠加/刷新/最大层数限制，属性实时动态计算
- 所有随机操作使用**固定种子 `Random`**，实现**确定性战斗回放**——同种子产生完全一致的战斗过程，天然支持录像系统和反外挂校验
- 实现伤害计算公式：基础攻防差 × 兵种克制（步→骑→弓→步，1.5 倍）× 暴击倍率 × 技能修正 × 护盾吸收

### 六、其他业务模块

- **登录注册**：MySQL 账号验证 → Redis 缓存加载玩家数据 → `SessionUtil` 绑定 Channel Attribute → `PlayerManager`（ConcurrentHashMap）加入在线列表 → 推送离线消息，完整闭环
- **建筑经营**：Redis Hash 存储建筑数据，升级扣除资源并记录 `finishTime` 时间戳，查询时检测到期自动结算
- **好友社交**：MySQL 双向关系存储，状态机管理申请流程（申请中→已通过/拒绝），好友列表含实时在线状态，好友请求服务端主动推送
- **Spring 轻量集成**：仅使用 `spring-context`（非 Spring Boot），`@ComponentScan` 自动扫描 + `@Autowired` 注入，与 Netty 手写路由器无缝配合

### 七、联调与测试

- 开发 **1900+ 行 HTML5 Canvas 战斗模拟器**（`battle-test.html`），像素风动画实时渲染战斗过程，通过 `BattleHttpTestServer`（JDK HttpServer）暴露 REST API 联调后端引擎
- 编写 8 个测试文件，覆盖全流程集成测试 (`FullIntegrationTest`)、控制台测试客户端 (`ConsoleTestClient`)、鉴权测试、建筑模块测试等

---

## 技术亮点总结

| 亮点 | 具体实现 |
|:---|:---|
| **数据驱动战斗引擎** | 策略 + 工厂 + 模板方法 + 组合 4 种设计模式，30+ 技能 / 13 种 Buff 全由 JSON 配置驱动，零代码扩展 |
| **确定性战斗回放** | 固定种子 Random 保证同输入同输出，天然支持录像回放与反外挂校验 |
| **Redis GEO 实现 AOI** | 原生 GEORADIUS 空间索引 + CompletableFuture 异步广播，避免阻塞 I/O 线程 |
| **AC 自动机敏感词过滤** | 手写 Aho-Corasick 算法，O(n) 线性时间多模式匹配 |
| **双层存储与双重落库** | Redis 缓存 + MySQL 持久化 + isDirty 脏标记 + 定时/断线双重落库 |
| **Netty Pipeline 鉴权** | AuthenticationHandler 零侵入式网关鉴权，业务层无感知 |
| **注解驱动路由** | 自定义 @GameHandler + 反射 MessageDispatcher，O(1) 消息分发 |
| **并发安全设计** | ConcurrentHashMap 在线管理 + Jedis try-with-resources + Channel Attribute 会话绑定 |
| **全栈联调能力** | 1900 行 Canvas 可视化战斗调试器 + REST API，前后端独立联调 |

---

## 系统架构图

```
    Client (TCP / Protobuf)
            │
            ▼
┌──────────────────────────────────────────────────┐
│                 Netty Pipeline                   │
│  IdleStateHandler → ProtobufVarint32Decoder →    │
│  ProtobufDecoder → AuthenticationHandler →       │
│  ServerHandler                                   │
└────────────────────┬─────────────────────────────┘
                     │
              MessageDispatcher (自定义 @GameHandler 注解路由)
                     │
       ┌─────────────┼───────────────┐
       ▼             ▼               ▼
  Controller    Controller      Controller     ← 10 个业务模块
       │             │               │
       ▼             ▼               ▼
    Service       Service         Service      ← 业务逻辑
       │             │               │
       ▼             ▼               ▼
      Dao           Dao             Dao        ← 数据访问
       │             │               │
       ▼             ▼               ▼
  Redis ←→ MySQL   Redis ←→ MySQL  Redis      ← 双层存储
```

---

## 技术关键词

Java 17 · Netty · Protobuf · Redis GEO · MySQL · HikariCP · Spring Context · Aho-Corasick · AOI · 回合制战斗引擎 · 数据驱动 · 策略模式 · 工厂模式 · 模板方法 · 确定性回放 · Buff 系统 · ConcurrentHashMap · CompletableFuture · JUnit 5
# 拾刻 · SleepReady

> 一款单机离线的睡前 / 生活记录 Android 应用。  
> 把日程、灵感、定时入睡（定时开启或者关闭手机某个功能如蓝牙、手电筒、飞行模式等，本项目因安卓安全管理原因暂时未全部完成）和值得记下的瞬间，收进一个干净温暖的小本子。
联系作者可提供最终版（最终版需改写手机某些权限，注意安全）

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-v7-FFB300)](https://developer.android.com/training/data-storage/room)
[![Offline](https://img.shields.io/badge/Network-Offline_Only-success)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 目录

- [项目简介](#项目简介)
- [设计哲学](#设计哲学)
- [功能特性](#功能特性)
  - [印记 · 日程板块](#印记--日程板块)
  - [心动 · 探索板块](#心动--探索板块)
  - [入梦 · 睡前定时板块](#入梦--睡前定时板块)
  - [时光 · 记录板块](#时光--记录板块)
  - [拾句 · 古诗板块](#拾句--古诗板块)
  - [全局能力](#全局能力)
- [应用截图与板块导览](#应用截图与板块导览)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [核心架构](#核心架构)
- [入梦自动化分层执行](#入梦自动化分层执行)
- [日程提醒可靠性体系](#日程提醒可靠性体系)
- [权限与隐私](#权限与隐私)
- [数据存储](#数据存储)
- [数据备份与恢复](#数据备份与恢复)
- [构建与运行](#构建与运行)
- [开发约定](#开发约定)
- [常见问题](#常见问题)
- [路线图](#路线图)
- [贡献](#贡献)
- [相关文档](#相关文档)
- [License](#license)

---

## 项目简介

**拾刻** 是一款定位为「睡前与日常仪式」的离线 Android 应用，包含 5 个相互独立又互相补完的板块：

| 板块 | 用途 |
|------|------|
| **印记**（Schedule） | 日程 / 生日 / 纪念日 / 倒数日，公历 / 农历无缝切换，可靠的系统级提醒与闹钟铃声 |
| **心动**（Explore） | 收集「想做的事」——想学 / 想看 / 想读 / 想做四类标签，三态状态机（想去做 → 在路上 → 已抵达） |
| **入梦**（Sleep Timer） | 多套睡前定时方案，到点自动调整 13 项系统开关；支持 A/B 类分层执行与宏录制 |
| **时光**（Moment） | 写下值得记下的瞬间：标题 / 时间 / 正文 / 多图，长按菜单删除，整库备份导出 |
| **拾句**（Verse） | 古诗收藏与赏析，衬线书卷气排版 |

---

## 设计哲学

- **单机离线**：`AndroidManifest` 不声明 `INTERNET` 权限，应用没有任何网络访问能力，杜绝隐私上传。
- **温暖低饱和**：默认深色主题搭配 _晨曦 / 林间 / 墨夜_ 三套配色方案，圆角统一 16dp，间距 8dp 倍数。
- **属性驱动正确性**：核心逻辑（定时上限、Cancel 不写历史、ActionPlan 序列化、Engine 输出长度恒为 13）以可执行 properties 表达，由 PBT（Property-Based Test）验证。
- **就地控制**：进度可视、过程可中断、操作可撤销，不弹出复杂的现代对话框。
- **国产 ROM 友好**：通知渠道齐备 + 精确闹钟降级 + 锁屏全屏 Intent + 一键跳系统设置引导，到点真的能响。

---

## 功能特性

### 印记 · 日程板块

- 四种类型：**日程 / 生日 / 纪念日 / 倒数日**
- 时间字段全部可选，时分支持滚轮选择 + 手动输入
- **公历 / 农历无缝切换**：列表与详情按"腊月""廿三""初五"等传统名显示
- 多个提醒节点（提前 3 天 / 1 天 / 3 小时…）
- 重复规则：不重复 / 每天 / 每周 / 每月 / 每年 + 自定义间隔
- 关键字搜索（标题 / 备注 / 地点）
- **可靠提醒**：基于 `AlarmManager.setExactAndAllowWhileIdle`，并在缺少 `SCHEDULE_EXACT_ALARM` 时自动降级到 `setAndAllowWhileIdle`，保底准时提醒
- **闹钟强提醒** 开关：开启后到点会启动 `AlarmRingService`（前台服务 + WakeLock）持续响铃 + 震动；锁屏下通过 `USE_FULL_SCREEN_INTENT` 拉起 `AlarmRingActivity`，必须用户手动停止
- **铃声可替换**：默认资源 `app/src/main/res/raw/alarm_sound.mp3`（小写），用户替换该文件即可换铃声
- **重启 / 升级自动重排**：`BootCompletedReceiver` + `MyPackageReplacedReceiver` 自动重新调度所有未触发的提醒
- **权限引导卡片**：当通知 / 精确闹钟 / 全屏 Intent / ROM 自启动等关键权限缺失时，在 `ScheduleScreen` 顶部展示 `PermissionGuideCard`，一键跳系统设置

### 心动 · 探索板块

- 三态状态机：**想去做 → 在路上 → 已抵达**
- 多选分类：想学 / 想看 / 想读 / 想做（创建后可改）
- **重写后的滑动手势**：
  - _想去做_ 只能向右滑入 _在路上_
  - _在路上_ 可以左滑回 _想去做_，也可以右滑入 _已抵达_
  - _已抵达_ 只能左滑回 _在路上_
  - 严格跟手，松手时若超过 1/3 宽度则吸附到下一态，否则弹回
  - **归档（删除）改为长按菜单触发**，与状态切换手势彻底分离
- **Random Pick**：从想去做池里随机抽一条卡片，附"换一个""去学习"按钮
- 自然语言输入栏：标题 + 备注 / 链接

### 入梦 · 睡前定时板块

- **多预设方案**：可保存"深度睡眠""午休""起床"等多套配置，同一时间最多一套运行
- **倒计时**：1 分钟 ~ 24 小时，时长支持小时 / 分钟两条滑块
- **保持屏幕常亮**：倒计时界面 `keepScreenOn = true` + 半透明遮罩降低亮度，不刺眼也不息屏，无障碍服务到点能可靠操作开关
- **13 项开关分层执行**：
  - **A 类（静默）**：勿扰、震动、省电、护眼、手电筒、屏幕录制
  - **B 类（引导）**：Wi-Fi、蓝牙、飞行模式、移动数据、定位、NFC
- **分层策略**（默认）：到点先执行 A 类；B 类等用户解锁后通过 `USER_PRESENT` 广播触发
- **统一策略**：到点不执行任何开关，全部等解锁后一次完成
- **+15 分钟**：上限 120 分钟（property P11）
- **取消不留痕**：Cancel 路径不写入执行历史（property P12）
- **宏录制（B 方案）**：通过无障碍服务记录用户在系统设置中的点击坐标 / 节点 / 延迟，绑定到方案；回放时优先用宏，失败再降级到通用引导跳转
- 桌面 Widget 一键启动上次方案

### 时光 · 记录板块

- 标题（必填）+ 时间（可选，滚轮选择）+ 正文（可选）+ 图片（最多 9 张）
- **完全禁止水平滑动**，只保留点击进入详情，长按弹菜单删除（避免误触）
- **图片查看器**：全屏 Dialog + HorizontalPager + 双指捏合缩放（1× ~ 5×）+ 双击切换 1× ↔ 2.5×
- 列表卡片首图作为缩略图，多图显示「+N」角标，加占位图与错误图
- 详情页：单图全宽 240dp，多图 2 列 1:1 网格
- 单条 **纯文本分享**（标题 + 时间 + 正文）

### 拾句 · 古诗板块

- 列表卡片与详情页采用 **衬线 Serif 字体**，行间距 1.8 倍，字号略大，营造书卷气
- 卡片背景半透明 / 轻纹理，与暖色深色系融合
- 详情页：标题 / 作者 / 朝代之间清晰层次；赏析区域以左边框 + 引号装饰与正文区分
- 适当留白，避免拥挤

### 全局能力

- **设置页**：主题模式（浅色 / 深色 / 跟随系统）+ 配色方案 + 入梦自动化策略 + 数据备份 + 精确提醒入口 + 从备份恢复
- **三套配色**：晨曦（默认）/ 林间 / 墨夜
- **左滑 / 系统返回**：回到上一层而不是直接退到首页
- **TopAppBar 设置入口**：四个主板块的标题栏右上角直达设置
- **统一字号层级**：标题 20sp / 正文 16sp / 辅助文字 13sp
- **过渡动画**：页面切换、按钮点击、卡片出现统一 200ms 淡入 / 缩放
- **空状态**：列表无数据时展示温暖提示文字
- **Flow 安全收集**：所有 UI 都用 `collectAsStateWithLifecycle()`，自动随生命周期暂停 / 恢复，避免内存泄漏

---

## 应用截图与板块导览

> 截图待补，可在发布前以 `app/build/outputs/apk/debug/app-debug.apk` 安装到模拟器后录制。

四个根路由对应底部导航：

```
印记      心动     入梦         时光       拾句
schedule  inbox    sleep_timer  moment     verse
```

二级路由：`timer/preset/new`、`timer/preset/edit/{id}`、`timer/countdown`、`history`、`settings`、`reminder_permission_guide`、`alarm_ring`。

---

## 技术栈

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin（少量 Java：`LunarCalendar` / 搜索引擎 / 统计 / Verse 仓库 / docx 生成） | 1.9.24 |
| 构建 | AGP / Gradle Wrapper | 8.2.2 |
| UI | Jetpack Compose + Material 3 | BOM 2024.06.00 |
| 导航 | androidx.navigation:navigation-compose | 2.7.7 |
| 异步 | kotlinx.coroutines + Flow | 1.8.1 |
| 存储 | Room（KAPT 编译） | 2.6.1 |
| 序列化 | kotlinx.serialization-json | 1.6.3 |
| 生命周期 | lifecycle-{viewmodel,runtime}-compose | 2.8.2 |
| 图片 | Coil for Compose | 2.6.0 |
| 测试 | JUnit 4 + Robolectric + room-testing + coroutines-test | — |

**仅 13 个 implementation 依赖**，没有 Hilt / Dagger / OkHttp / Retrofit。  
DI 由手写的 `AppContainer` 提供，方便审阅、易于裁剪。

---

## 项目结构

```
app/src/main/java/com/example/sleepready/
├── MainActivity.kt              单 Activity 容器；MediaProjection / POST_NOTIFICATIONS launcher
├── SleepReadyApp.kt             Application；初始化 AppContainer / 主题 / 启动时重排闹钟
│
├── di/
│   └── AppContainer.kt          手写 DI 容器（Database / Repos / Service 单例）
│
├── domain/model/                领域模型
│   ├── Schedule.kt / ScheduleType.kt / RepeatRule.kt
│   ├── Moment.kt / KnowledgeItem.kt / VerseItem.kt
│   ├── TimerConfig.kt / TimerPreset.kt
│   ├── ActionPlan.kt / Enums.kt（13 项 SwitchItem 枚举 + 执行顺序）
│   └── ExecutionEntry.kt / ExecutionHistory.kt / LunarCalendar.kt
│
├── data/
│   ├── db/                      Room：7 张表 + 7 个 DAO + Converters
│   │   ├── AppDatabase.kt（v7：fallbackToDestructiveMigration）
│   │   └── Entities.kt / *Dao.kt
│   └── prefs/                   PreferencesRepository（ui_prefs / runtime_prefs 双文件）
│
├── repository/                  仓库层（全部 Flow 暴露）
│   ├── KnowledgeRepository.kt
│   ├── MomentRepository.kt / MomentBackup.kt
│   ├── ScheduleRepository.kt（含 getAllActive 用于重启重排）
│   ├── TimerPresetRepository.kt / TimerConfigRepository.kt
│   ├── HistoryRepository.kt
│   ├── MacroRepository.kt（宏录制方案绑定）
│   └── BackupRepository.kt（全库 zip 导出 / 导入）
│
├── permission/
│   ├── PermissionState.kt / PermissionMatrix.kt / PermissionManager.kt
│   ├── ReminderPermissionChecker.kt   汇总通知 / 精确闹钟 / 全屏 Intent / ROM 检测
│   ├── ReminderPermissionGuideScreen.kt 引导页 UI
│   ├── PermissionGuideState.kt / PermissionGuideItem.kt
│   ├── ROMDetector.kt          华为 / 小米 / OPPO / VIVO ROM 嗅探与跳转 Intent
│   └── DegradedReminder.kt     通知被拒时的降级提醒（toast / 系统通知）
│
├── service/
│   ├── SleepReadyTimerService.kt   倒计时前台服务 + AlarmManager 兜底
│   ├── ExecutionEngine.kt          按 SwitchItem.executionOrder 顺序执行 ActionPlan
│   ├── SystemActions.kt            13 项开关的具体实现 + 引导跳转
│   ├── ScreenRecordingController.kt MediaProjection 进程级单例
│   ├── TimerStateHolder.kt         StateFlow<TimerState> + OneShotEvent
│   ├── AlarmReceiver.kt            ACTION_TIMEOUT 广播回服务
│   ├── UnlockReceiver.kt           USER_PRESENT 触发 B 类开关回放
│   ├── RestoreReceiver.kt          系统重置后恢复定时
│   ├── notif/
│   │   └── TimerNotifications.kt   倒计时 / 执行中 / 汇总 / 引导提示通知
│   ├── macro/                      宏录制 / 回放（无障碍服务，可选）
│   │   ├── MacroAccessibilityService.kt
│   │   └── MacroRecorder.kt / MacroPlayer.kt
│   └── schedule/                   日程提醒子系统
│       ├── ScheduleReminderScheduler.kt   AlarmManager 调度（含降级 fallback）
│       ├── ScheduleReminderReceiver.kt    收到广播 → 通知 / 启 AlarmRingService
│       ├── AlarmRingService.kt            前台 + WakeLock + 持续响铃 / 震动
│       ├── AlarmRingActivity.kt           锁屏全屏 Intent UI
│       ├── AlarmRescheduleHelper.kt       一次性重排所有 active 提醒
│       ├── BootCompletedReceiver.kt       开机后调用 rescheduleAllAsync
│       └── MyPackageReplacedReceiver.kt   应用更新后调用 rescheduleAllAsync
│
├── search/                       全局搜索引擎（Java）
│   ├── SearchableItem.java / SearchEngine.java
│   └── SearchDaoBridge.kt
│
├── statistics/                   数据统计（Java）
│   ├── StatisticsCalculator.java / StatisticsRepository.java
│
├── verse/                        拾句板块仓库
│   ├── VerseRepository.java
│   └── VerseDaoBridge.kt
│
├── lunar/
│   └── LunarCalendar.java        农历转换核心（公历 ↔ 农历）
│
├── ui/
│   ├── nav/                      Routes + SleepReadyNavGraph
│   ├── theme/                    Color / Type（衬线 + 字号层级）/ Shape / SleepReadyTheme
│   ├── common/                   WheelPicker / DateTimeWheelSheet / SwipeableRow…
│   ├── schedule/                 印记板块（含 ScheduleEditor / TypePicker / PermissionGuideCard）
│   ├── inbox/                    心动板块（含 RandomPickBottomSheet / 重写后的 SwipeableRow）
│   ├── timer/                    入梦板块（PresetList / PresetEditor / Countdown）
│   ├── moment/                   时光板块（含 ImageViewerDialog 全屏图片查看器，长按菜单删除）
│   ├── verse/                    拾句板块（衬线书卷气排版）
│   ├── history/                  入梦历史
│   ├── search/                   全局搜索界面
│   ├── statistics/               统计可视化
│   └── settings/                 设置页（备份 / 恢复 / 主题 / 精确提醒）
│
└── widget/
    ├── SleepReadyWidgetProvider.kt 桌面小部件
    └── WidgetReceiver.kt           ACTION_QUICK_START → 打开倒计时
```

---

## 核心架构

### 分层

```
UI (Compose Screen + ViewModel)
    │
    │ collectAsStateWithLifecycle / events SharedFlow
    ▼
Repository (KnowledgeRepository, MomentRepository, ScheduleRepository, ...)
    │
    │ Room Flow  /  SharedPreferences  /  filesDir
    ▼
Data Source (AppDatabase, PreferencesRepository, MomentBackup, BackupRepository)
```

- 单 Activity + Compose Navigation，所有屏幕在 `SleepReadyNavGraph` 注册
- ViewModel 通过 `viewModelFactory` 由 `AppContainer` 注入仓库
- 仓库返回 `Flow<T>`；UI 用 `collectAsStateWithLifecycle()` 订阅，自动随生命周期暂停 / 恢复
- 一次性事件（`Saved`、`Failed`、`LimitReached`）走 `SharedFlow`，UI 用 `LaunchedEffect` 监听

### 入梦定时服务

```
TimerConfigRepository
        │
        ▼
ContextCompat.startForegroundService(ACTION_START)
        │
        ▼
SleepReadyTimerService
  ├─ Handler 每秒 tick → TimerStateHolder.update + 通知
  ├─ AlarmManager.setExactAndAllowWhileIdle  ──┐
  └─ AtomicBoolean finished 防双触发           │
                                               ▼
                                      AlarmReceiver
                                               │
                                               ▼
                              ACTION_TIMEOUT 回服务
                                               │
                                               ▼
                                  ExecutionEngine.run(plan)
                                               │
                                               ▼
                                  HistoryRepository.insert
                                               │
                                               ▼
                                  buildSummary 通知 + Finished 状态
```

**关键属性**（design.md 中定义为 P1–P15）：

- **P11**：连续 +15 分钟后 `totalMs ≤ 120 * 60_000`
- **P12**：Cancel 路径不写入历史
- **P7-P10**：ExecutionEngine 输出长度恒为 13；NONE 项 SKIPPED；非 NONE 项之间 ≥ 300ms；某项异常不影响其余
- **P15**：TimerConfig 写入后 `load()` 等价于 `c.normalized()`

---

## 入梦自动化分层执行

Android 10+ 收紧了第三方应用对系统硬件的控制权，**Wi-Fi / 飞行模式 / 移动数据 / 定位 / NFC** 没有公开 API 可静默切换；锁屏状态下无障碍服务也无法注入触摸事件。

拾刻把 13 项开关明确分类，再以「分层执行」绕过这两个限制：

| 类别 | 开关 | 实现路径 |
|------|------|----------|
| **A 类（静默）** | 勿扰 | `NotificationManager.setInterruptionFilter` |
| | 震动 / 静音 | `AudioManager.setRingerMode` |
| | 省电模式 | `Settings.Global` 引导（部分机型） |
| | 护眼模式 | `Settings.System` 写入 |
| | 手电筒 | `CameraManager.setTorchMode` |
| | 屏幕录制 | `MediaProjection.stop()`（需用户先授予 token） |
| **B 类（引导）** | Wi-Fi / 蓝牙（API 33+）/ 飞行 / 数据 / 定位 / NFC | 跳转设置页 `Settings.ACTION_*_SETTINGS` + 通知提示 |

**默认策略（分层执行）**：

1. 倒计时归零 → 立刻执行所有 A 类
2. 检查 `KeyguardManager.isDeviceLocked`：
   - 已解锁：B 类立即引导
   - 锁屏中：发汇总通知"X 项已完成，剩余 Y 项将在解锁后自动继续"，写 `pendingGuidedPlan` 到 `runtime_prefs`
3. `UnlockReceiver` 监听 `USER_PRESENT`：用户解锁瞬间读取 pending 计划并回放

**统一策略**：到点不执行任何开关，全部等解锁后通过 `UnlockReceiver` 一次完成。

### 宏录制（B 方案）

由于不同厂商系统设置页面节点 ID 不一致，通用跳转方案在国产 ROM 上易失败。拾刻提供「录制宏」入口：

1. 用户点击 _录制宏_，应用启动无障碍服务并悬浮提示
2. 用户在系统设置中按顺序手动完成所有开关切换；服务记录每步的坐标 / 节点 ID / 延迟
3. 录制结束后宏与方案绑定，存入 `MacroRepository`
4. 回放时优先按宏序列执行；任一步失败则降级到通用 `Settings.ACTION_*_SETTINGS` 跳转

宏录制基于 `AccessibilityService`，默认关闭，由用户在设置中显式启用。

---

## 日程提醒可靠性体系

到点提醒在国产 ROM 上极易被静默拦截。拾刻通过 6 项保护确保「到点真的能响」：

| 保护层 | 实现 |
|--------|------|
| **精确闹钟** | `AlarmManager.setExactAndAllowWhileIdle`，无 `SCHEDULE_EXACT_ALARM` 时降级到 `setAndAllowWhileIdle` |
| **唯一 PendingIntent** | data URI 包含 `scheduleId` + `instant`，避免相同时刻被覆盖 |
| **重启重排** | `BootCompletedReceiver` 监听 `BOOT_COMPLETED` → `AlarmRescheduleHelper.rescheduleAllAsync` |
| **升级重排** | `MyPackageReplacedReceiver` 监听 `MY_PACKAGE_REPLACED`，应用更新后自动恢复全部闹钟 |
| **降级提醒** | 通知权限被拒时，`DegradedReminder` 直接显示 Toast / 系统级广播兜底 |
| **闹钟铃声** | 开启「闹钟强提醒」开关后启动 `AlarmRingService`（前台 + WakeLock）持续响铃 / 震动；锁屏下用 `USE_FULL_SCREEN_INTENT` 拉起 `AlarmRingActivity`，必须用户手动停止 |

### 权限引导卡片

`ReminderPermissionChecker` 周期性扫描以下条件：

- `NotificationManagerCompat.areNotificationsEnabled()`
- `AlarmManager.canScheduleExactAlarms()`（API 31+）
- `NotificationManager.canUseFullScreenIntent()`（API 34+）
- ROM 自启动 / 后台保护（`ROMDetector` 检测华为 / 小米 / OPPO / VIVO / Realme / 一加 / 魅族）

任一条件未满足，`ScheduleScreen` 顶部展示 `PermissionGuideCard`，列出所有缺失项，每条提供"前往设置"按钮，跳转目标 Intent 由 `ROMDetector.openAutoStartSettings()` 等方法生成。

### 闹钟铃声更换

默认资源：

```
app/src/main/res/raw/alarm_sound.mp3   ← 文件名必须小写，否则 AAPT 会报错
```

替换步骤：

1. 准备一段不超过 30 秒的 mp3 音频
2. 重命名为 `alarm_sound.mp3`（**全小写**）
3. 覆盖到 `app/src/main/res/raw/` 目录
4. 重新构建 APK

代码中以 `R.raw.alarm_sound` 引用，不需要其他改动。

---

## 权限与隐私

用户视角的说明见 [PRIVACY.md](PRIVACY.md)。下面是实现对照表。

### 声明的权限

| 权限 | 用途 | 时机 |
|------|------|------|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | 倒计时 / 闹钟铃声前台服务 | 启动方案或闹钟时 |
| `POST_NOTIFICATIONS` | API 33+ 通知（倒计时 / 汇总 / 引导 / 日程提醒） | OnboardingDialog 触发 |
| `SCHEDULE_EXACT_ALARM` + `USE_EXACT_ALARM` | Doze 兜底 + 日程提醒精确触发 | 自动；缺失时降级 |
| `RECEIVE_BOOT_COMPLETED` | 开机后重排所有日程闹钟 | 自动 |
| `WAKE_LOCK` | 闹钟铃声期间防 CPU 休眠 | 开启「闹钟强提醒」开关时 |
| `USE_FULL_SCREEN_INTENT` | 锁屏全屏拉起 `AlarmRingActivity` | 同上 |
| `DISABLE_KEYGUARD` + `TURN_SCREEN_ON` | 闹钟唤醒屏幕、解除锁屏 | 同上 |
| `CAMERA` | 手电筒（`setTorchMode` 需要此权限） | ActionPlanList 选启用时 |
| `BLUETOOTH_CONNECT` + `BLUETOOTH_ADMIN` | 蓝牙开关（API < 33） | 同上 |
| `ACCESS_NOTIFICATION_POLICY` | 勿扰模式 | 同上 |
| `VIBRATE` | 日程提醒震动 | 自动 |
| `WRITE_SETTINGS` / `WRITE_APN_SETTINGS` | 引导式跳转目标声明（不会被授予） | 仅 manifest |

> **明确未声明** `android.permission.INTERNET`：app 没有任何网络访问能力。

### 权限子系统

```kotlin
sealed interface PermissionState {
    data object Granted
    data class DeniedRuntime(val permission: String)
    data class DeniedSpecial(val intent: Intent)
    data object NotRequired
}
```

- `PermissionMatrix` 把 `(SwitchItem, SwitchAction)` 映射到 `PermissionRequirement`
- `PermissionManager` 在 `MainActivity` 中预注册 `RequestPermission` launcher，监听 `ON_RESUME` 自动 `refresh()`
- Compose 树通过 `LocalPermissionManager` CompositionLocal 注入
- `ReminderPermissionChecker` 专门处理日程提醒相关 4 项条件，状态由 `PermissionGuideState` 持有，UI 通过 `PermissionGuideCard` 展示

---

## 数据存储

### Room 数据库（v7）

```
sleep_ready.db
├── knowledge_item          心动条目
├── execution_history       入梦执行历史（含 entries JSON）
├── timer_config            (legacy 单一配置，兼容保留)
├── timer_preset            多预设方案
├── schedule_item           日程 / 生日 / 纪念日 / 倒数日（含 useAlarmRing）
├── moment_item             时光记录（图片路径以 JSON 数组存储）
└── verse_item              拾句板块古诗收藏
```

版本演进（来自 `AppDatabase.kt` doc）：

- **v4**：新增 `TimerPresetEntity`、`MomentEntity`
- **v5**：`KnowledgeItemEntity` 加 `imagePathsJson`；`TimerPresetEntity` 加 `sortIndex / pinned / autoRestore*`
- **v6**：新增 `VerseEntity`（拾句板块）
- **v7**：`ScheduleEntity` 加 `useAlarmRing` 字段（闹钟强提醒开关）

未发布期间使用 `fallbackToDestructiveMigration()`，正式版本会逐步补充 migration。

### SharedPreferences 双文件

- **`ui_prefs.xml`**：主题模式 / 配色方案 / 入梦策略 / onboarding 标志 / 上次时长
- **`runtime_prefs.xml`**：当前运行 preset id / pending 引导计划 / pending history id

旧版 `sleep_ready_prefs.xml` 在首次启动时自动迁移到新文件后即可删除。

### 文件目录

- 时光图片：`filesDir/moments/<momentId>/<uuid>.jpg`（应用私有，卸载即清空）
- 全库备份：API 29+ 写入 `Downloads/SleepReady/`，旧版写 `filesDir/exports/`

---

## 数据备份与恢复

`BackupRepository.kt` 实现了**全库 zip 备份**，覆盖所有板块（印记 / 心动 / 入梦 / 时光 / 拾句 / 历史 / 预设）。

### 导出（设置 → 备份）

打包内容：

1. Room 数据库三件套（运行时切到 WAL 模式时会同时存在）
   - `sleep_ready.db`
   - `sleep_ready.db-shm`
   - `sleep_ready.db-wal`
2. 应用私有目录 `filesDir/` 下的所有附件（含时光图片、宏录制等）

输出路径：

- API 29+：通过 `MediaStore` 写入 `Downloads/SleepReady/sleep_ready_backup_yyyymmdd_hhmmss.zip`
- 旧版：写入 `filesDir/exports/`（成功后给出绝对路径）

### 恢复（设置 → 从备份恢复）

1. 用户通过 SAF 选择 zip 文件
2. **双重确认对话框**警告将完整替换现有数据
3. 校验 zip 结构 → 关闭数据库 → 解压回 `databaseDir` 与 `filesDir`
4. 通过 `killAndRestart()` 强制重启进程，新数据生效

整个过程不会触网，也不会写其他外部目录。

---

## 构建与运行

```bash
git clone https://github.com/<YOUR_USERNAME>/SleepReady.git
cd SleepReady
```

用 Android Studio 打开根目录，等待 Gradle Sync 完成后即可运行。命令行构建见下。

### 环境要求

- **JDK 17+**（AGP 8.2.2 要求）
- **Android Studio Hedgehog 2023.1.1** 或更新
- Android SDK 34
- Windows / macOS / Linux 均可，PowerShell 脚本仅 Windows 可用

### 一次性准备（Windows）

```powershell
# 项目内置的 _build.ps1 会清理 GRADLE_OPTS 后调用 gradlew.bat
# 直接命令行也可
$env:GRADLE_OPTS = ''
.\gradlew.bat assembleDebug
```

### 常用命令

| 任务 | 命令 |
|------|------|
| Debug APK | `gradlew assembleDebug` |
| 单元测试 | `gradlew testDebugUnitTest` |
| 构建 + 安装到设备 | `gradlew installDebug` |
| 清理 | `gradlew clean` |

构建产物：`app/build/outputs/apk/debug/app-debug.apk`（约 19 MB）。

### Windows 一键脚本

```powershell
.\_build.ps1
```

- 把日志写入 `build_log.txt`
- 把退出码写入 `build_exit.txt`（成功 = `EXITCODE=0`）

---

## 开发约定

### 代码风格

- 单 Activity + Compose；ViewModel 持有状态，Composable 只负责渲染
- 仓库层使用 `Flow<T>`；状态用 `StateFlow`；事件用 `SharedFlow`（`extraBufferCapacity = 1`）
- 不在 UI 直接操作 DB；不在仓库直接持有 Context（必要时传 `applicationContext`）
- 主题相关常量集中在 `ui/theme/`，elevation 默认 `1dp / 2dp` 高级简约风
- 圆角统一 `RoundedCornerShape(16.dp)`，间距 8dp 倍数
- 字号层级：标题 20sp / 正文 16sp / 辅助 13sp

### 添加新表 / 字段

1. 在 `data/db/Entities.kt` 新增 `@Entity`
2. 在 `data/db/AppDatabase.kt` 的 `entities = [...]` 中追加，并 `version + 1`
3. 新建 DAO 接口
4. 写 `repository/XxxRepository.kt` 暴露 `Flow` 接口
5. 在 `di/AppContainer.kt` 注册
6. ViewModel 在 `viewModelFactory` 中注入

### 添加新开关

1. 在 `domain/model/Enums.kt` 的 `SwitchItem` 中添加枚举
2. 在 `executionOrder` 列表加入到合适的位置
3. 在 `service/SystemActions.kt` 实现 `applyXxx(action)`
4. 在 `permission/PermissionMatrix.kt` 声明 `PermissionRequirement`
5. UI 自动出现在 `ActionPlanList`（无需改 UI）

### 添加新日程提醒类型

1. 在 `domain/model/ScheduleType.kt` 增加枚举
2. `ScheduleReminderScheduler` 增加 trigger 时间计算分支
3. UI 在 `ScheduleEditor.TypePicker` 增加选项
4. 重启重排逻辑 `AlarmRescheduleHelper.rescheduleAllAsync` 自动覆盖（基于 `getAllActive()`）

---

## 常见问题

**Q：为什么应用包名仍是 `com.example.sleepready`？**  
A：项目早期沿用，作者明确希望保留。如需上架商店再改。

**Q：为什么不用 Hilt？**  
A：13 个依赖、几个核心仓库的小项目，手写 `AppContainer` 比 Hilt 注解更直观，编译也更快。

**Q：B 类开关在国产 ROM 上无障碍点击不生效？**  
A：不同厂商的设置页节点 ID 不同，宏录制 + 回放可针对当前手机定制；默认行为是跳转设置页 + 通知提示，由用户手动点。

**Q：日程到点没反应？**  
A：先打开 `ScheduleScreen` 顶部的权限引导卡片，按提示授权通知 / 精确闹钟 / 自启动；国产 ROM 还需在系统设置里把拾刻加入「自启动白名单 + 后台高耗电允许 + 锁屏后台运行」。

**Q：闹钟铃声没声音？**  
A：检查 `app/src/main/res/raw/alarm_sound.mp3` 是否存在且**文件名全小写**。AAPT 对 raw 资源大小写敏感，`alarm_sound.MP3` 会构建失败或运行时找不到。

**Q：图片备份占用空间太大？**  
A：导出 zip 后可手动清理 `filesDir/moments/`；正式版会加增量备份与压缩选项。

**Q：构建报 `This foundation API is experimental`？**  
A：`HorizontalPager` 在 Compose foundation 1.6 上仍是 `@ExperimentalFoundationApi`，已在 `ImageViewerDialog.kt` 加 `@OptIn` 注解。

**Q：`gradlew.bat` 报 `GRADLE_OPTS` 错？**  
A：在 PowerShell 中执行 `$env:GRADLE_OPTS = ''` 后再跑，或直接用 `_build.ps1`。

**Q：恢复备份后应用闪退？**  
A：恢复流程会调用 `killAndRestart()` 强制结束进程让新数据库接管。如果 zip 来自旧版本（schema 较低），由于使用 `fallbackToDestructiveMigration` 数据会被清空。建议同版本之间互导。

---

## 路线图

- [ ] iCal 导入 / 导出（仅本地文件，不联网）
- [ ] 时光增量备份与压缩
- [ ] 心动条目支持图片附件
- [ ] 入梦方案模板分享（导出为本地 JSON）
- [ ] WCAG 对比度审计
- [ ] Migration 取代 `fallbackToDestructiveMigration`
- [ ] 拾句离线诗词包扩充

---

## 贡献

欢迎提 Issue 和 Pull Request。较大改动请先开 Issue 对齐方案。

请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)（环境、分支、测试、PR 清单）和 [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)。

核心约束：**不要引入网络访问，不要声明 `INTERNET` 权限，不要把用户数据默认送出设备。**

## 相关文档

| 文件 | 用途 |
|------|------|
| [CONTRIBUTING.md](CONTRIBUTING.md) | 如何构建、分支约定、PR 清单 |
| [PRIVACY.md](PRIVACY.md) | 数据存在哪、权限用途、备份边界 |
| [SECURITY.md](SECURITY.md) | 漏洞私下报告与加固约定 |
| [CHANGELOG.md](CHANGELOG.md) | 版本变更 |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | 社区行为准则 |
| [docs/github-publish.md](docs/github-publish.md) | 第一次推到 GitHub 的步骤 |

## License

[MIT](LICENSE) © 2026 拾刻 SleepReady Contributors

---

> _收下今天的拾刻，把心安顿在一刻钟之内。_

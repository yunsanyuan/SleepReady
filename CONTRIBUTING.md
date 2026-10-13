# 贡献指南

感谢你愿意改进 **拾刻 · SleepReady**。本仓库是一款单机离线的 Android 应用，贡献前请先读完 [README](README.md)，尤其是「设计哲学」「开发约定」和「权限与隐私」。

## 开始之前

- 不要引入任何网络请求库（OkHttp、Retrofit、Firebase、统计 SDK 等）。
- 不要在 `AndroidManifest.xml` 中声明 `android.permission.INTERNET`。
- 不要把用户数据写到外部存储以外的位置；备份只走设置页里的本地 zip。
- 不要提交 `local.properties`、密钥库、`.hprof`、构建产物或 IDE 缓存。

如果你不确定改动是否符合「离线 / 就地控制 / 国产 ROM 友好」这三条原则，先开 Issue 讨论。

## 环境要求

| 项目 | 版本 |
|------|------|
| JDK | 17+（AGP 8.2.2 要求） |
| Android Studio | Hedgehog 2023.1.1 或更新 |
| Android SDK | compileSdk / targetSdk **34**，minSdk **26** |
| Gradle Wrapper | 8.4（仓库已提供 `gradlew`） |

Windows / macOS / Linux 均可。Windows 上若 `gradlew.bat` 因 `GRADLE_OPTS` 报错，先执行：

```powershell
$env:GRADLE_OPTS = ''
.\gradlew.bat assembleDebug
```

或直接运行项目根目录的 `_build.ps1`。

## 本地构建

```bash
git clone https://github.com/<YOUR_USERNAME>/SleepReady.git
cd SleepReady
```

用 Android Studio 打开根目录，等待 Gradle Sync 完成后：

```bash
# Debug APK
./gradlew assembleDebug

# 单元测试
./gradlew testDebugUnitTest

# 安装到已连接设备 / 模拟器
./gradlew installDebug

# 清理
./gradlew clean
```

产物位置：`app/build/outputs/apk/debug/app-debug.apk`。

首次在新机器上构建时，确认 `gradle/wrapper/gradle-wrapper.jar` 存在。若缺失，在已安装 Gradle 的环境执行：

```bash
gradle wrapper --gradle-version 8.4
```

## 分支与提交

从默认分支拉出功能分支，不要直接往 `main` / `master` 推未经评审的大改：

| 前缀 | 用途 | 示例 |
|------|------|------|
| `feat/` | 新功能 | `feat/verse-import` |
| `fix/` | 缺陷 | `fix/lunar-offset` |
| `docs/` | 文档 | `docs/contributing` |
| `refactor/` | 重构（行为不变） | `refactor/app-container` |
| `test/` | 仅测试 | `test/timer-p11` |

提交信息建议用 [Conventional Commits](https://www.conventionalcommits.org/)：

```
feat(schedule): 农历生日支持闰月
fix(timer): Cancel 路径不再写入执行历史
docs: 补充权限引导说明
```

一条提交只做一件事。提交信息写「为什么」，不要只写「改了 xxx」。

## 开发约定（摘要）

完整约定见 [README · 开发约定](README.md#开发约定)。这里只列评审时最常卡住的几点：

1. **单 Activity + Compose**。ViewModel 持有状态，Composable 只负责渲染。
2. **仓库返回 `Flow<T>`**；UI 用 `collectAsStateWithLifecycle()`。一次性事件走 `SharedFlow`（`extraBufferCapacity = 1`）。
3. **不要在 UI 直接操作 DB**；仓库不要长期持有 Activity `Context`，必要时传 `applicationContext`。
4. **依赖注入用手写 `AppContainer`**，不要引入 Hilt / Dagger。
5. **主题常量集中在 `ui/theme/`**：圆角 `16.dp`，间距 8dp 倍数，字号 20 / 16 / 13sp。
6. **Room 未发布阶段**仍可使用 `fallbackToDestructiveMigration()`；一旦对外发版，新增字段必须写 `Migration` 并提升 `version`。

### 添加新表 / 字段

1. `data/db/Entities.kt` 增加 `@Entity`
2. `AppDatabase.kt` 的 `entities` 追加，`version + 1`
3. 新建 DAO
4. `repository/XxxRepository.kt` 暴露 `Flow`
5. `di/AppContainer.kt` 注册
6. ViewModel 在 `viewModelFactory` 中注入

### 添加新开关

1. `domain/model/Enums.kt` 的 `SwitchItem` 增加枚举
2. 加入 `executionOrder`
3. `service/SystemActions.kt` 实现 `applyXxx`
4. `permission/PermissionMatrix.kt` 声明权限需求
5. UI 会自动出现在 `ActionPlanList`

### 添加新日程类型

1. `domain/model/ScheduleType.kt` 增加枚举
2. `ScheduleReminderScheduler` 增加触发时间计算
3. `ScheduleEditor.TypePicker` 增加选项
4. 重启重排走 `AlarmRescheduleHelper.rescheduleAllAsync`，一般不用改

## 测试

当前单元测试在 `app/src/test/`，至少覆盖农历换算 `LunarCalendarTest`。

入梦模块的核心正确性以属性（P1–P15）描述，见 README「核心架构」。新增定时 / 历史 / Engine 相关逻辑时，优先补可执行断言，例如：

- **P11**：连续 +15 分钟后 `totalMs ≤ 120 * 60_000`
- **P12**：Cancel 路径不写入执行历史
- **P7–P10**：Engine 输出长度恒为 13；单项失败不影响其余

提交前至少跑：

```bash
./gradlew testDebugUnitTest
```

UI 改动请在模拟器或真机走一遍相关板块，并检查其它读取同一状态的页面有没有回归。涉及日程提醒时，还要看权限引导卡片、精确闹钟和国产 ROM 自启动说明是否仍然成立。

## Pull Request

1. 先开 Issue（缺陷 / 功能），较大改动先对齐方案再写代码。
2. 保持 PR 小而完整：一个主题、可独立评审、可独立回滚。
3. 按 [Pull Request 模板](.github/PULL_REQUEST_TEMPLATE.md) 填写。
4. 自检清单：
   - [ ] 没有新增联网能力或 `INTERNET` 权限
   - [ ] 没有把密钥、`local.properties`、用户数据提交上来
   - [ ] Room / 备份路径的兼容性已考虑
   - [ ] 相关测试通过
   - [ ] README / CHANGELOG 需要同步时已更新

维护者可能会请你拆分过大的 PR，或补充截图 / 录屏（尤其是 Compose UI）。

## 文档

- 用户能感知的行为变化：更新 [README](README.md) 与 [CHANGELOG](CHANGELOG.md)
- 权限、备份、隐私相关：同步 [PRIVACY](PRIVACY.md)
- 安全相关：看 [SECURITY](SECURITY.md)，不要在公开 Issue 里贴可利用细节

## 行为准则

参与本仓库即表示你同意遵守 [行为准则](CODE_OF_CONDUCT.md)。

package report;

import java.util.ArrayList;
import java.util.List;

/**
 * 「拾刻」项目报告内容。所有段落以结构化数据存储，由 DocxGenerator 渲染为 .docx。
 *
 * 段落类型：
 *   H1   一级标题（章）
 *   H2   二级标题（节）
 *   H3   三级标题（小节）
 *   P    正文段落（首行缩进 2 字）
 *   BUL  无序列表项
 *   CODE 代码块（等宽字体，无缩进）
 *   PB   分页符
 *   TBL  表格（首行表头）
 *   COVER 封面块（占满一页）
 */
public final class ReportContent {

    public static final class Block {
        public final String type;
        public final String text;
        public final List<String> headers;
        public final List<List<String>> rows;

        Block(String type, String text) {
            this.type = type;
            this.text = text;
            this.headers = null;
            this.rows = null;
        }

        Block(String type, List<String> headers, List<List<String>> rows) {
            this.type = type;
            this.text = null;
            this.headers = headers;
            this.rows = rows;
        }
    }

    public static List<Block> build() {
        List<Block> b = new ArrayList<>();
        // ---------- 封面 ----------
        b.add(c("COVER", null));

        // ---------- 摘要 ----------
        h1(b, "摘要");
        p(b, "「拾刻」是一款面向个人日常仪式的 Android 单机离线应用。应用包含五大板块：印记（日程／生日／纪念日／倒数日）、心动（想做的事）、入梦（睡前定时与开关自动化）、时光（图文记录）、拾句（古诗收藏）。整体定位强调「温暖低饱和的视觉」「就地控制的交互」「无网络无收集的隐私」三大原则。");
        p(b, "本项目以 Kotlin 与 Jetpack Compose 为主要 UI 实现语言，配合 Room 持久化、Coroutines + Flow 响应式数据流、kotlinx.serialization 序列化，并在核心算法层（农历换算、全文搜索引擎、统计聚合、古诗仓库）使用 Java 实现，形成 Kotlin/Java 混合的工程结构。应用全程不申请 INTERNET 权限，所有数据保存在设备本地的 Room 数据库与应用私有目录下。");
        p(b, "本报告将依次从需求分析、系统与界面设计、功能实现、关键算法、系统测试、部署运行、心得体会七个角度展开，对项目从立项到完成的全过程进行系统总结。");
        p(b, "关键词：Android、Jetpack Compose、单机离线、农历换算、自动化执行、属性测试");
        b.add(c("PB", null));

        // ---------- 第一章 需求分析 ----------
        h1(b, "第一章 需求分析");
        h2(b, "1.1 项目背景");
        p(b, "现代人在睡前往往同时面对多种琐事：要写下今天的感悟、要把待办的想法记下来、要逐个关闭手机开关进入勿扰模式、要查看明天的安排。市面上已有的笔记、日历、自动化、勿扰类应用往往侧重某一方向，相互之间数据割裂；同时大量应用要求用户登录账号并把数据上传云端，与「睡前的私密时刻」并不契合。");
        p(b, "拾刻的目标是把「夜晚的 15 分钟仪式」收进一个干净温暖的小本子：一个应用同时承担日程提醒、想法收集、入梦定时、生活记录与古诗陪伴，所有数据保留在本地，不需要任何账号体系，断网也能完整运行。");

        h2(b, "1.2 用户群体与典型场景");
        p(b, "目标用户为关心仪式感、希望减少手机干扰的中文用户，年龄 18 至 40 岁，对个人隐私有较高敏感度，习惯使用国产手机（小米、华为、OPPO、vivo 等定制 ROM）。");
        p(b, "典型场景：用户在 22:50 拿起手机，从拾刻底部导航选择入梦板块，挑选一套 30 分钟的「深度睡眠」预设方案，点击启动后熄屏入睡；倒计时归零时应用自动开启勿扰、关闭手电筒，并把锁屏状态下无法直接操作的 Wi-Fi、蓝牙等开关挂起，等用户次日早晨解锁瞬间继续完成。早晨醒来用户翻看「时光」板块写下昨夜入睡前的一段思绪并附一张窗外的照片，再到「印记」板块查看今日待办，到「拾句」板块翻开一首随机古诗作为开篇。");

        h2(b, "1.3 功能性需求");
        p(b, "本节按板块罗列应用的功能性需求。需求条目按照「编号 + 简述 + 验收要点」的方式描述，便于后续的设计与测试追溯。");

        h3(b, "1.3.1 印记板块（日程）");
        bul(b, "F-S-01 创建四种类型条目：日程／生日／纪念日／倒数日，可指定标题、时间、地点、备注、提醒节点、重复规则。");
        bul(b, "F-S-02 时间字段允许公历／农历切换，列表与详情用「腊月廿三」「闰七月初五」等传统名显示。");
        bul(b, "F-S-03 顶部支持按全部／今天／本周／本月四档时间窗口筛选，并按时间窗口分组渲染列表标题。");
        bul(b, "F-S-04 倒数日列表卡片右侧自动显示「还有 N 天」「就是今天」「已过期」；纪念日显示「第 N 天」。");
        bul(b, "F-S-05 多个提醒节点（提前 3 天 / 1 天 / 3 小时）通过 AlarmManager 触发系统通知与震动。");
        bul(b, "F-S-06 支持按标题／备注／地点的关键词搜索。");

        h3(b, "1.3.2 心动板块（探索）");
        bul(b, "F-K-01 创建条目时需选择至少一个分类（想学／想看／想读／想做），支持多选。");
        bul(b, "F-K-02 三态状态机：想去做 → 在路上 → 已抵达，右滑切换、左滑归档（5 秒撤销）。");
        bul(b, "F-K-03 条目可附最多 3 张图片，列表卡片显示首图缩略，详情弹窗左右滑动查看全图。");
        bul(b, "F-K-04 备注字段 15 字以内显示在卡片标题下方作为预览。");
        bul(b, "F-K-05 提供「随机抽一条」功能，附「换一个 / 去学习 / 关闭」三按钮。");

        h3(b, "1.3.3 入梦板块（睡前定时）");
        bul(b, "F-T-01 支持创建多套预设方案，每套独立保存名称、时长、动作清单。");
        bul(b, "F-T-02 倒计时支持 1 分钟到 24 小时，时长用小时与分钟两条滑块设置。");
        bul(b, "F-T-03 13 项系统开关分两类：A 类（静默执行）勿扰／静音／振动／省电／护眼／手电筒／屏幕录制；B 类（引导执行）Wi-Fi／蓝牙／飞行模式／移动数据／定位／NFC。");
        bul(b, "F-T-04 默认分层执行策略：到点立即执行 A 类；锁屏中的 B 类等到 USER_PRESENT 解锁广播时回放。可在设置切换到「全部等待解锁」策略。");
        bul(b, "F-T-05 +15 分钟续时支持，上限 120 分钟；触顶时弹出 Snackbar 提示并不变更状态。");
        bul(b, "F-T-06 取消倒计时不写入历史记录；唯有正常超时执行完毕才插入一条历史。");
        bul(b, "F-T-07 长按预设方案可置顶、上移、下移、编辑、删除。");
        bul(b, "F-T-08 起床后自动恢复：用户可设定恢复时间（如次日 7:00），到点自动反向执行被改变的开关。");

        h3(b, "1.3.4 时光板块（记录）");
        bul(b, "F-M-01 创建条目时可填写标题（必填）、时间（可选）、正文（可选）、图片（最多 9 张）。");
        bul(b, "F-M-02 详情页支持图片全屏查看：HorizontalPager 翻页、双指捏合缩放（1×~5×）、双击切换 1×↔2.5×。");
        bul(b, "F-M-03 列表右滑显示删除按钮，需用户两次确认才会真正删除。");
        bul(b, "F-M-04 详情页可分享纯文本（标题 + 时间 + 正文）。");
        bul(b, "F-M-05 设置页支持把整个时光数据库与图片打包为 zip 导出到系统下载目录。");

        h3(b, "1.3.5 拾句板块（古诗）");
        bul(b, "F-V-01 收录、查看、编辑、删除诗句条目，必填字段为诗题、作者、朝代、全文。");
        bul(b, "F-V-02 顶部按朝代筛选（全部／先秦／汉／魏晋／南北朝／唐／宋／元／明／清／近现代／其他）。");
        bul(b, "F-V-03 提供「随机一首」按钮，跳转详情页全屏展示。");
        bul(b, "F-V-04 详情页展示标题、朝代、作者、全文与赏析。");

        h3(b, "1.3.6 全局功能");
        bul(b, "F-G-01 设置页提供主题模式（浅色／深色／跟随系统）与三套配色（晨曦／林间／墨夜）。");
        bul(b, "F-G-02 设置页提供全局搜索入口，可跨印记／心动／时光／拾句搜索，结果按板块分组展示并高亮匹配文字。");
        bul(b, "F-G-03 设置页提供数据统计入口，展示各板块条目数、最近 7 天活动柱状图、连续打卡天数、心动状态分布、印记类型分布、拾句朝代 Top 5。");
        bul(b, "F-G-04 桌面 Widget 一键启动上次入梦方案。");

        h2(b, "1.4 非功能性需求");
        h3(b, "1.4.1 隐私与离线");
        p(b, "AndroidManifest 不声明 android.permission.INTERNET，从根本上杜绝任何网络请求；所有用户数据保留在 Room 数据库（应用私有数据目录）与 filesDir 下的图片缓存目录。卸载应用即清除全部数据。");
        h3(b, "1.4.2 性能与响应");
        p(b, "冷启动时间在中端机型上控制在 1.5 秒以内；列表滚动需保持 60 FPS；倒计时通知每秒更新一次而不阻塞 UI。");
        h3(b, "1.4.3 可用性");
        p(b, "默认深色主题搭配低饱和暖色调避免长时间观看刺眼；字号最小 12sp，正文 15sp 以上；左滑或系统返回手势始终回到上一层而不是直接退出应用。");
        h3(b, "1.4.4 可维护性");
        p(b, "采用 Repository / ViewModel / Composable 三层结构；DI 通过手写 AppContainer 实现，依赖关系全部 lazy 注入，便于 Stub 替换；核心算法（农历、搜索、统计）以无副作用的纯函数 / 同步方法形式提供，便于单元测试与属性测试。");

        h2(b, "1.5 关键约束与正确性属性");
        p(b, "在设计阶段抽出 15 条 Correctness Property（P1-P15）作为「不变量」贯穿实现与测试。本节列出与用户体验最直接相关的若干条，供后续测试章节回溯：");
        bul(b, "P11：连续 +15 分钟操作后，倒计时总时长 totalMs 永远小于等于 120 分钟。");
        bul(b, "P12：取消（Cancel）路径不写入执行历史；只有正常 Timeout 才插入一条历史。");
        bul(b, "P13：执行历史中 entries 序列长度恒为 13，且与 SwitchItem.executionOrder 顺序严格相等。");
        bul(b, "P14：主题模式持久化无歧义，重启应用后用户设置仍然生效。");
        bul(b, "P15：TimerConfig 写入 → 读取 → 解析后等价于原值（durationMinutes 与 plan.normalized() 相等）。");
        b.add(c("PB", null));

        // ---------- 第二章 系统设计 ----------
        h1(b, "第二章 系统设计");

        h2(b, "2.1 总体架构");
        p(b, "应用采用单 Activity + Compose Navigation 的现代 Android 架构。整体分为四层：UI 层（Compose Screen + ViewModel）、Repository 层、数据源层（Room AppDatabase + SharedPreferences + filesDir）、系统服务层（前台 Service、AlarmManager、BroadcastReceiver、AccessibilityService）。");
        p(b, "依赖方向严格自上而下：UI 不直接访问数据库；Repository 不持有 UI 状态；Service 与 ViewModel 通过 TimerStateHolder 提供的单向 StateFlow + SharedFlow 通信，确保单源数据流。");
        code(b,
            "UI (Compose Screen + ViewModel)\n" +
            "    | collectAsStateWithLifecycle / events SharedFlow\n" +
            "    v\n" +
            "Repository (KnowledgeRepository, MomentRepository, ScheduleRepository,\n" +
            "            TimerPresetRepository, HistoryRepository, VerseRepository[Java])\n" +
            "    | Flow<T>\n" +
            "    v\n" +
            "Data Source (AppDatabase Room v6, PreferencesRepository,\n" +
            "             MomentBackup, filesDir/{moments,knowledge}/)\n" +
            "    |\n" +
            "    v\n" +
            "System Service (SleepReadyTimerService, ExecutionEngine,\n" +
            "                AlarmReceiver, UnlockReceiver, RestoreReceiver,\n" +
            "                MacroAccessibilityService, ScheduleReminderReceiver)");

        h2(b, "2.2 包结构");
        p(b, "包结构按职责划分，便于新模块的归类和老代码的查找：");
        code(b,
            "com.example.sleepready\n" +
            "├── MainActivity.kt              单 Activity 容器\n" +
            "├── SleepReadyApp.kt             Application；主题与 AppContainer 初始化\n" +
            "├── di/AppContainer.kt           手写 DI 容器\n" +
            "├── domain/model/                领域模型（含 LunarCalendar.kt 适配层）\n" +
            "├── data/db/                     Room 实体与 DAO\n" +
            "├── data/prefs/                  PreferencesRepository（双文件）\n" +
            "├── repository/                  Kotlin 仓库（含 MomentBackup）\n" +
            "├── permission/                  权限矩阵与 PermissionManager\n" +
            "├── service/                     SleepReadyTimerService 等\n" +
            "│   ├── macro/                   宏录制与回放（无障碍）\n" +
            "│   ├── notif/                   通知子系统\n" +
            "│   └── schedule/                日程提醒调度\n" +
            "├── ui/                          Compose UI 与 ViewModel\n" +
            "│   ├── nav/                     Routes + SleepReadyNavGraph\n" +
            "│   ├── theme/                   Color / Type / Shape / SleepReadyTheme\n" +
            "│   ├── common/                  WheelPicker / DateTimeWheelSheet\n" +
            "│   ├── schedule / inbox / timer / moment / verse / history\n" +
            "│   ├── statistics / search / settings\n" +
            "├── widget/                      桌面 Widget\n" +
            "├── lunar/LunarCalendar.java     Java 农历核心算法\n" +
            "├── verse/                       Java 拾句仓库 + Kotlin 桥接\n" +
            "├── search/                      Java 搜索引擎 + Kotlin 桥接\n" +
            "└── statistics/                  Java 统计仓库与计算器");

        h2(b, "2.3 数据库设计");
        p(b, "应用使用 Room 持久化数据，数据库版本目前为 v6，包含 7 张表。未发布期间使用 fallbackToDestructiveMigration() 加快迭代，正式版本将补充 Migration 链。");
        List<List<String>> dbRows = new ArrayList<>();
        dbRows.add(row("schedule_item", "印记条目", "id, title, type, priority, targetTimeMillis, endTimeMillis, isAllDay, targetIsLunar, hasYear, location, repeatRule, weekdaysMask, monthlyDay, reminderOffsetsMinutesJson, note, createdAt, updatedAt"));
        dbRows.add(row("knowledge_item", "心动条目", "id, title, category(逗号分隔), link, note, status, imagePathsJson, createdAt, updatedAt"));
        dbRows.add(row("moment_item", "时光记录", "id, title, body, eventTimeMillis, imagePathsJson, createdAt, updatedAt"));
        dbRows.add(row("verse_item", "拾句条目", "id, title, author, dynasty, content, notes, createdAt"));
        dbRows.add(row("timer_preset", "入梦预设方案", "id, name, durationMinutes, planJson, lastUsedAt, sortIndex, pinned, autoRestoreEnabled, autoRestoreHour, autoRestoreMinute, createdAt, updatedAt"));
        dbRows.add(row("timer_config", "当前运行中配置（单行）", "id=1, durationMinutes, planJson, updatedAt"));
        dbRows.add(row("execution_history", "入梦执行历史", "id, startedAt, finishedAt, durationMinutes, entriesJson"));
        b.add(table(headers("表名", "用途", "主要字段"), dbRows));
        p(b, "JSON 存储字段（imagePathsJson、planJson、entriesJson、reminderOffsetsMinutesJson）通过 kotlinx.serialization 与 Room 的 TypeConverter 协同工作，避免引入额外关联表。");

        h2(b, "2.4 模块划分");
        List<List<String>> modRows = new ArrayList<>();
        modRows.add(row("印记 Schedule", "Kotlin", "ScheduleRepository / ScheduleViewModel / ScheduleScreen / ScheduleEditor"));
        modRows.add(row("心动 Knowledge", "Kotlin", "KnowledgeRepository / KnowledgeInboxViewModel / BottomInputBar / KnowledgeItemCard / SwipeableRow / RandomPickBottomSheet / EditKnowledgeDialog"));
        modRows.add(row("入梦 Timer", "Kotlin + Java", "TimerPresetRepository / TimerPresetListViewModel / SleepReadyTimerService / ExecutionEngine / SystemActions / RestoreReceiver / UnlockReceiver"));
        modRows.add(row("时光 Moment", "Kotlin", "MomentRepository / MomentBackup / MomentViewModel / MomentScreen / ImageViewerDialog / MomentDetailScreen"));
        modRows.add(row("拾句 Verse", "Java + Kotlin", "VerseRepository.java / VerseDaoBridge.kt / VerseViewModel / VerseScreen / VerseDetailScreen / VerseEditorScreen"));
        modRows.add(row("农历 Lunar", "Java", "LunarCalendar.java（1900-2100 查表 + 干支 + 生肖 + 24 节气）"));
        modRows.add(row("搜索 Search", "Java + Kotlin", "SearchEngine.java / SearchableItem.java / SearchDaoBridge.kt / SearchViewModel / SearchScreen"));
        modRows.add(row("统计 Statistics", "Java + Kotlin", "StatisticsRepository.java / StatisticsCalculator.java / StatisticsViewModel / StatisticsScreen"));
        modRows.add(row("权限 Permission", "Kotlin", "PermissionMatrix / PermissionManager / PermissionState / OnboardingDialog"));
        modRows.add(row("通知 Notif", "Kotlin", "TimerNotifications（4 个 NotificationChannel）"));
        modRows.add(row("Widget", "Kotlin", "SleepReadyWidgetProvider / WidgetReceiver"));
        b.add(table(headers("模块", "语言", "主要类"), modRows));

        h2(b, "2.5 Java 与 Kotlin 互操作策略");
        p(b, "为提升 Java 代码参与度并把对类型与算法要求最高的部分交给 Java，本项目采用以下混合策略：");
        bul(b, "核心算法用 Java：农历换算（LunarCalendar.java）使用大型常量表 + 数学计算，Java 写起来更直观且测试友好。");
        bul(b, "核心仓库与引擎用 Java：VerseRepository、SearchEngine、StatisticsRepository、StatisticsCalculator 全部 Java 实现，全方法同步阻塞返回 List 而非 Flow。");
        bul(b, "Kotlin 桥接 suspend：在 Java 与 suspend DAO 之间引入薄薄的 Kotlin 桥接器（VerseDaoBridge、SearchDaoBridge），用 runBlocking 把协程方法包成阻塞调用；Java 层只看到普通同步方法。");
        bul(b, "ViewModel 在 Dispatchers.IO 调用 Java：Kotlin ViewModel 通过 withContext(Dispatchers.IO) 调用 Java 仓库，避免阻塞主线程，同时保留 Compose 的协程协作能力。");
        b.add(c("PB", null));

        // ---------- 第三章 界面设计 ----------
        h1(b, "第三章 界面设计");

        h2(b, "3.1 设计原则");
        bul(b, "温暖低饱和：默认深色主题，主色调避免冷蓝；提供晨曦／林间／墨夜三套配色方案。");
        bul(b, "高级简约：Card 全部使用 1dp 默认 elevation、2dp 按下 elevation；圆角统一 16dp；分隔线使用 outlineVariant 浅色。");
        bul(b, "就地控制：底部导航五个 root 页面，每个 root 页右上角直达设置；二级页面用全屏 Overlay 模拟而不是新 Activity，左滑或系统返回回到上一层。");
        bul(b, "渐进披露：心动页底部输入栏默认折叠为「+ 添加」按钮；时光与印记的图片放大、农历切换、自动恢复时间等功能都通过开关或弹层惰性显示。");
        bul(b, "中文友好：字体优先级微软雅黑 → Calibri → 系统默认；正文 16sp 以上保证阅读舒适。");

        h2(b, "3.2 配色系统");
        p(b, "三套配色都遵循 Material 3 ColorScheme，提供 primary / secondary / tertiary / surface / surfaceVariant 等完整角色，支持系统自动切换浅色与深色。各方案的核心色调如下：");
        List<List<String>> colorRows = new ArrayList<>();
        colorRows.add(row("晨曦 Dawn（默认）", "柔和浅紫 #B79FE8", "深靛 #1F1B2E", "兼顾白天与夜晚使用"));
        colorRows.add(row("林间 Forest", "清新翠绿 #5EAD7A", "墨绿 #18241D", "自然清醒"));
        colorRows.add(row("墨夜 Midnight", "淡紫高亮 #9D8DF1", "墨蓝 #0F132A", "专为卧室低光场景"));
        b.add(table(headers("方案", "主色 Primary", "背景 Surface", "适用场景"), colorRows));

        h2(b, "3.3 排版与组件规范");
        p(b, "正文 bodyMedium = 16sp，bodyLarge = 18sp，标题 titleMedium = 18sp Semibold，倒计时 displayLarge = 96sp Monospace。组件层面：Card 圆角 16dp、按钮圆角 28dp（pill）、缩略图圆角 8dp（小）/ 12dp（中）/ 16dp（大）。");

        h2(b, "3.4 导航结构");
        p(b, "应用使用 Compose Navigation 单 Activity 管理路由：");
        code(b,
            "                  +-----------------+\n" +
            "   底部导航 ----> | 印记 心动 入梦 时光 拾句 |\n" +
            "                  +-----------------+\n" +
            "                          |\n" +
            "        +------+------+------+------+\n" +
            "        v      v      v      v      v\n" +
            "   schedule  explore sleep_timer  moment  verse\n" +
            "                              |\n" +
            "            +-----------------+-----------------+\n" +
            "            v                                   v\n" +
            "    timer/preset/new                    timer/preset/edit/{id}\n" +
            "    timer/countdown                     verse/detail/{id}\n" +
            "    history                             verse/new\n" +
            "    settings                            verse/edit/{id}\n" +
            "        |\n" +
            "        +-> search       全局搜索\n" +
            "        +-> statistics   数据统计");
        p(b, "底部导航根据当前路由决定显示与否：所有 root 路由（5 个）显示底部导航；二级路由（设置、搜索、统计、各类编辑器）通过 nonTopLevelRoutes 集合显式排除以避免动画期间闪烁。");

        h2(b, "3.5 关键页面布局");
        h3(b, "3.5.1 印记列表");
        p(b, "顶部 TopAppBar 标题「印记」，右上角设置图标。下方依次为搜索框、日期范围 Chips（全部／今天／本周／本月）、类型 Chips（全部类型／日程／生日／纪念日／倒数日）。列表区按时间窗口分组显示 sticky header（如「今天」「本周」「已过期」），每条卡片由类型图标、标题、时间副标题、地点行、右上角关键摘要（如「还有 28 天」）构成。FAB 文字 + 图标按钮「添加」。");

        h3(b, "3.5.2 心动列表");
        p(b, "TopAppBar 含「随机一条」与「设置」两个按钮。下方 FilterChip 行展示 5 个分类（全部 / 想学 / 想看 / 想读 / 想做）。条目卡片左侧是状态彩色圆点，紧接 40dp 圆角缩略图（若有图片），右侧标题 + 备注预览（≤15 字 + 省略号）+ 分类与状态副标题。底部常驻 BottomInputBar，默认折叠为一个圆角主色按钮，展开后是标题 → 多选分类 → 备注 → 链接 → 图片 → 保存。");

        h3(b, "3.5.3 入梦倒计时");
        p(b, "BreathingHaloBackground 提供深暖渐变 + 呼吸光晕动画，CountdownDigits 用 96sp Monospace 显示 MM:SS 并附加暖色 glow。中部展示 ActionPlan 摘要（仅列出非 NONE 项及目标动作），底部「取消」与「+15 分钟」两个按钮。监听 OneShotEvent.LimitReached 弹出 Snackbar「已达 120 分钟上限」。");

        h3(b, "3.5.4 时光详情与图片放大");
        p(b, "详情页 TopAppBar 含分享、编辑、删除三个按钮。正文区按标题 → 元信息（事件时间 · 创建时间）→ 分隔线 → 正文 → 图片网格的顺序。点击任意图片打开 ImageViewerDialog：全屏黑色背景、HorizontalPager 水平翻页、双指捏合缩放（scale 1f..5f）、双击切换 1×↔2.5×、单击关闭。顶部条显示当前页码与关闭按钮。");

        h3(b, "3.5.5 拾句详情");
        p(b, "诗题居顶（headlineSmall + Semibold），「朝代 · 作者」一行副标题，分隔线下展示全文（bodyLarge），最后是赏析（在浅色卡片内 bodyMedium）。TopAppBar 右上角「随机」按钮直接跳转到下一首。");

        h3(b, "3.5.6 全局搜索");
        p(b, "顶部 OutlinedTextField + 搜索图标，输入即触发 200ms debounce 后调用 Java SearchEngine。结果按板块分组（印记／心动／时光／拾句），每组 SectionHeader 显示「板块 · 命中数」，结果卡片用 AnnotatedString + SpanStyle(背景色淡黄) 高亮匹配子串。点击结果跳转到对应板块（拾句深链到详情页）。");

        h3(b, "3.5.7 数据统计");
        p(b, "顶部双卡片显示总条目与连续打卡天数。下方依次为「各板块条目」「心动状态分布」「印记类型分布」「拾句朝代 Top 5」「最近 7 天活动」。前 4 个分组用 LinearProgressIndicator + 数字渲染条形比例；最近 7 天活动是自绘的简单柱状图（每个柱子高度按 max 归一化）。");
        b.add(c("PB", null));

        // ---------- 第四章 功能设计与实现 ----------
        h1(b, "第四章 功能设计与实现");

        h2(b, "4.1 印记板块的实现要点");
        p(b, "印记板块的核心是 Schedule 领域模型，按 ScheduleType 区分四种类型，复用同一个 ScheduleEditor 但通过 when (type) 渲染不同字段集。提醒节点用 List<Long> 表示「提前多少分钟」，写入数据库时通过 Converters 序列化为 JSON 数组。");
        p(b, "日程提醒由 ScheduleReminderScheduler 负责安排：每条印记的每个 reminderOffset 都会单独注册一个 PendingIntent，requestCode = scheduleId * MAX_REMINDERS + offsetIndex 保证唯一。AlarmManager.setExactAndAllowWhileIdle 提供 Doze-resilient 触发。ScheduleReminderReceiver 收到广播后用 NotificationManagerCompat 弹出系统通知 + 震动。");

        h2(b, "4.2 心动板块的实现要点");
        p(b, "心动板块允许多分类：DB 仍用单列 category 存储「逗号分隔 storageValue」，Repository 在编解码时维持 KnowledgeCategory.values() 的声明顺序，保证写入字符串的稳定性。条目图片复制到 filesDir/knowledge/<uuid>.jpg，最多 3 张；删除条目时同步清理对应文件。");
        p(b, "SwipeableRow 基于 Material 3 SwipeToDismissBox 实现：左滑直接归档（DONE 状态），右滑用 SegmentedButton 提示三态切换（IDLE/PAUSED/DONE）。监听 dismissValue 通过 distinctUntilChanged() 防止 LaunchedEffect 重复触发。");

        h2(b, "4.3 入梦板块的实现要点");
        h3(b, "4.3.1 SleepReadyTimerService 状态机");
        p(b, "前台服务承担倒计时核心。onStartCommand 根据 Intent action 分发到 4 个处理器：");
        bul(b, "ACTION_START：startForeground 占位通知 → 异步加载 TimerConfig → 启动 1 秒 ticker → 注册 AlarmManager.setExactAndAllowWhileIdle 兜底。");
        bul(b, "ACTION_CANCEL：取消 alarm + ticker → stopForeground(REMOVE) → 不写历史 → TimerStateHolder.reset()。");
        bul(b, "ACTION_EXTEND：totalMs + 15min ≤ 120min 时更新 endAt 并重建 alarm；否则 emit OneShotEvent.LimitReached 不变更状态。");
        bul(b, "ACTION_TIMEOUT：CAS finished.compareAndSet(false, true) 防双触发 → 切换到执行通知 → ExecutionEngine.run(plan) → 写一条 ExecutionHistory → 切换到 summary 通知 → stopForeground(DETACH)。");

        h3(b, "4.3.2 双触发兜底机制");
        p(b, "Handler 每秒 tick 既要更新 UI（通过 TimerStateHolder.update），又要在 remaining ≤ 0 时主动调用 handleTimeout；同时 AlarmManager 也会在 endAt 触发广播回 Service 调用 handleTimeout。两条路径同时跑可能导致 ExecutionEngine 跑两次、写两条历史。AtomicBoolean finished 的 compareAndSet 保证「先到的赢，后到的 no-op」。");

        h3(b, "4.3.3 分层执行策略");
        p(b, "ExecutionEngine 顺序按 SwitchItem.executionOrder 迭代 13 项开关，每一项根据 strategy 与当前 keyguard 状态决定立即执行还是延后：");
        code(b,
            "shouldDefer(item, strategy, isLockedNow):\n" +
            "    DEFER_ALL  -> true\n" +
            "    LAYERED    -> item.tier == GUIDED && isLockedNow");
        p(b, "延后执行的项用 DEFERRED 状态写入历史，同时把 (SwitchItem→SwitchAction) 序列化到 prefs.pendingGuidedPlan。UnlockReceiver 监听 ACTION_USER_PRESENT，解锁瞬间调用 ExecutionEngine.runDeferred 或 MacroPlayer 回放，并把回放结果用 HistoryRepository.replaceEntries 合并回原有历史记录的 DEFERRED 行。");

        h3(b, "4.3.4 起床后自动恢复");
        p(b, "当用户在预设方案中开启「起床后自动恢复」并设定时间（如次日 7:00），Service 在 ACTION_TIMEOUT 完成后会调用 buildReversePlan(plan) 把每一项 TURN_ON↔TURN_OFF 反向，再交给 RestoreScheduler 安排到指定时刻触发 RestoreReceiver。RestoreReceiver 收到广播后调用 ExecutionEngine.run(reversePlan, strategy, locked) 跑一次，完成后清空 prefs.autoRestorePlan。");

        h2(b, "4.4 时光板块的实现要点");
        p(b, "时光的图片完全离线：从系统 PhotoPicker（PickMultipleVisualMedia）选取 URI 后立即用 ContentResolver 把流复制到 filesDir/moments/<uuid>.jpg，存路径而非 URI，避免外部存储被改名后丢失引用。删除条目时通过 decode(imagePathsJson) 反序列化路径列表并 File.delete。");
        p(b, "ImageViewerDialog 用 androidx.compose.foundation.pager.HorizontalPager + 自实现的 ZoomableImage：detectTransformGestures 监听双指 pan 与 zoom，把 scale 限制在 [1f, 5f] 区间；offsetX/offsetY 在缩放后按 size.width * (scale - 1) / 2 限制，防止图片越界。双击通过 detectTapGestures 切换 1×↔2.5×，单击退出 Dialog。");
        p(b, "数据备份用 MomentBackup.exportToDownloads：把 moment_item 表序列化为 moments.json，与图片文件一起打包成 zip。Android 10+ 通过 MediaStore.Downloads API 写入；旧版本写到 filesDir/exports/。");

        h2(b, "4.5 拾句板块的实现要点（Java 实现）");
        p(b, "拾句板块从仓库到 Java 算法层面全部用 Java：VerseRepository.java 依赖 VerseDaoBridge.kt（用 runBlocking 把 suspend DAO 包成阻塞调用），所有方法返回 List<VerseItem> 或单个 VerseItem。Kotlin VerseViewModel 在 Dispatchers.IO 上调用 Java，再把结果发到 StateFlow 供 Compose 订阅。");
        code(b,
            "// VerseRepository.java（节选）\n" +
            "public List<VerseItem> getByDynasty(String dynasty) {\n" +
            "    if (dynasty == null || dynasty.isEmpty() || \"全部\".equals(dynasty)) {\n" +
            "        return getAll();\n" +
            "    }\n" +
            "    return toDomainList(bridge.getByDynastySync(dynasty));\n" +
            "}\n" +
            "\n" +
            "public VerseItem getRandom() {\n" +
            "    VerseEntity e = bridge.pickRandomSync();\n" +
            "    return e == null ? null : toDomain(e);\n" +
            "}");

        h2(b, "4.6 农历换算（Java 实现）");
        p(b, "LunarCalendar.java 的核心是 1900-2100 共 201 年的整数编码表 LUNAR_INFO[]，每个整数用低 4 位表示该年闰月（0=无闰）、bit16 表示闰月天数、高 12 位每位表示对应月份是否大月（30 天）。两个关键函数：");
        code(b,
            "// 农历 -> 公历 epoch ms\n" +
            "public static long lunarToSolarMillis(int year, int month, int day,\n" +
            "                                      boolean isLeap, int hour, int minute) {\n" +
            "    long totalDays = 0;\n" +
            "    for (int y = START_YEAR; y < year; y++) totalDays += daysInYear(y);\n" +
            "    int leap = leapMonth(year);\n" +
            "    for (int m = 1; m < month; m++) {\n" +
            "        totalDays += monthDays(year, m);\n" +
            "        if (m == leap) totalDays += leapDays(year);\n" +
            "    }\n" +
            "    if (isLeap && leap == month) totalDays += monthDays(year, month);\n" +
            "    totalDays += (day - 1);\n" +
            "    return LUNAR_BASE_MS + totalDays * MS_PER_DAY;\n" +
            "}");
        p(b, "此外还提供：");
        bul(b, "getGanZhiYear(year)：(year - 4) % 60 后查 HEAVENLY_STEMS[%10] + EARTHLY_BRANCHES[%12]。");
        bul(b, "getZodiac(year)：(year - 4) % 12 查 ZODIAC[]。");
        bul(b, "getSolarTerm(year, termIndex)：基准 1900-01-06 + 525948.76 * (year - 1900) + 偏移分钟数，得到任意年份任意节气的精确时刻。误差通常 ≤ 1 天。");

        h2(b, "4.7 全局搜索引擎（Java 实现）");
        p(b, "SearchEngine.java 通过 SearchDaoBridge 同步拉取 4 个板块的快照，对每条记录的相关字段做关键词匹配，按相关性评分排序后返回 List<SearchableItem>。评分规则：");
        bul(b, "标题完全匹配（去大小写）：100。");
        bul(b, "标题部分匹配（包含 query 或任一 token）：70。");
        bul(b, "正文 / 备注 / 地点 / 朝代 / 作者 等字段包含 query 或任一 token：40。");
        bul(b, "多 token 命中加成：每多一个 token 命中标题 +5。");
        code(b,
            "// SearchEngine.java search() 主流程节选\n" +
            "public List<SearchableItem> search(String query) {\n" +
            "    if (isBlank(query)) return Collections.emptyList();\n" +
            "    String[] tokens = tokenize(query);\n" +
            "    List<SearchableItem> out = new ArrayList<>();\n" +
            "    for (ScheduleEntity s : bridge.getSchedules()) {\n" +
            "        int score = scoreSchedule(s, query, tokens);\n" +
            "        if (score > 0) out.add(new SearchableItem(SCHEDULE, s.getId(), ...));\n" +
            "    }\n" +
            "    Collections.sort(out, (a, b) -> Integer.compare(b.getRelevance(), a.getRelevance()));\n" +
            "    return out;\n" +
            "}");

        h2(b, "4.8 数据统计（Java 实现）");
        p(b, "StatisticsCalculator.java 是无状态的纯算法工具：bucketByDay 按日分桶、groupByKey 按字段分组计数、topN 取前 N、streakDays 计算连续打卡天数。StatisticsRepository.java 复用 SearchDaoBridge 的同步 DAO 拉取，再交给 Calculator 聚合。");
        p(b, "连续打卡算法的实现要点是把 timestamps 全部归一到「当天 00:00」加入 HashSet，再从今天起步用 cursor -= 86400000 反向数到第一个不在 set 中的天数即停止。");

        h2(b, "4.9 权限矩阵与降级");
        p(b, "PermissionMatrix.requirementFor(item, action) 把 (SwitchItem, SwitchAction) 映射到 PermissionRequirement 三种结果之一：Runtime（需运行时权限）、SettingsIntent（需跳转特殊设置页）、None。PermissionManager 在 MainActivity 中预注册 RequestPermission launcher，通过 LocalPermissionManager CompositionLocal 注入 Compose 树。");
        p(b, "降级原则：所有 SystemActions 方法独立 try/catch，SecurityException → PERMISSION_DENIED；其它 Throwable → FAILED；永不抛出导致 Service 崩溃（对应 P10）。POST_NOTIFICATIONS 在 API 33+ 未授时所有通知 post 调用降级为 no-op，倒计时仍正常运行。");

        h2(b, "4.10 国产 ROM 兼容（无障碍降级）");
        p(b, "MacroAccessibilityService 提供两套节点查找：第一套按录制 NodeSignature（viewIdResourceName → text → contentDescription）精确匹配；找不到则触发降级路径 findToggleByKeywords：递归收集所有 className 后缀为 Switch / ToggleButton / SwitchCompat / SwitchMaterial / CompoundButton 的节点，按当前开关推断关键词列表（Wi-Fi / WLAN / 无线 / 蓝牙 / Bluetooth / 飞行 / Airplane / 移动数据 / Mobile data / 定位 / Location / NFC），对节点本身及向上 3 层祖先与同级文本做 contains 匹配，命中即点击。MacroPlayer 用 3 秒轮询（每 150ms 试一次）等待节点出现，超时再降级到坐标点击或返回 FAILED。");
        b.add(c("PB", null));

        // ---------- 第五章 系统测试 ----------
        h1(b, "第五章 系统测试");

        h2(b, "5.1 测试环境");
        p(b, "本项目采用「编译驱动」+「单元/属性测试」+「人工设备验证」三级测试体系。开发期间所有验证均在以下环境下进行：");
        List<List<String>> envRows = new ArrayList<>();
        envRows.add(row("编译工具", "Gradle 8.2.2 + Android Gradle Plugin 8.2.2 + Kotlin 1.9.24"));
        envRows.add(row("JDK", "JDK 17（Compose 与 AGP 8.x 要求）"));
        envRows.add(row("Compose BOM", "2024.06.00 / Compiler 1.5.14"));
        envRows.add(row("Room", "2.6.1（KAPT）"));
        envRows.add(row("单元测试框架", "JUnit 4.13.2 + kotlinx-coroutines-test + Robolectric 4.12.2 + room-testing"));
        envRows.add(row("设备", "Android 13 真机（小米 11）+ Android 14 模拟器（Pixel 6）"));
        b.add(table(headers("类别", "版本与说明"), envRows));

        h2(b, "5.2 编译与静态检查");
        p(b, "每次代码提交前都会运行 gradlew assembleDebug，确保 Kotlin、Java、Room KAPT、资源合并、AAPT 全部通过。Compose 1.6+ 的 ExperimentalFoundationApi（HorizontalPager / rememberPagerState）需要在调用方加 @OptIn 注解，否则编译会失败 —— 此问题在「时光图片放大」上线时遇到一次，加注解后解决。");
        p(b, "当前最新一次完整构建：");
        code(b,
            "> Task :app:packageDebug\n" +
            "> Task :app:createDebugApkListingFileRedirect UP-TO-DATE\n" +
            "> Task :app:assembleDebug\n" +
            "\n" +
            "BUILD SUCCESSFUL in 1m 10s\n" +
            "35 actionable tasks: 10 executed, 25 up-to-date\n" +
            "\n" +
            "  APK Length     LastWriteTime\n" +
            "  ----------     -------------\n" +
            "    18991963     2026-05-26 16:43:22");

        h2(b, "5.3 单元与属性测试");
        p(b, "在 design.md 中抽象出的 15 条 Correctness Property（P1-P15）大部分通过单元测试或属性测试覆盖。下列为关键测试用例：");
        List<List<String>> propRows = new ArrayList<>();
        propRows.add(row("P1", "空白 title 拒绝写入", "随机 100 个 title.isBlank() 输入，全部返回 Result.failure 且 DAO 行数不变。"));
        propRows.add(row("P3", "左滑归档可撤销", "KnowledgeRepository.setStatus 后 updatedAt 严格递增；undoArchive 恢复 prevStatus。"));
        propRows.add(row("P4", "pickRandom 只返回 pending", "随机 100 次混合状态集合，结果要么 null 要么 status == IDLE/PAUSED。"));
        propRows.add(row("P5", "ActionPlan JSON 往返", "随机 ActionPlan p → encode → decode 后 normalized() 等价。"));
        propRows.add(row("P6", "ScreenRecording.TURN_ON 退化", "随机用户输入 ActionPlan，normalized() 后 SCREEN_RECORDING 不可能为 TURN_ON。"));
        propRows.add(row("P7", "ExecutionEngine 输出顺序", "随机 ActionPlan → engine.run 输出长度恒为 13 且 switchItem 序列等于 SwitchItem.executionOrder。"));
        propRows.add(row("P8", "NONE 项 SKIPPED", "若 plan.map[s] == NONE，对应 entry.result == SKIPPED 且 message == null。"));
        propRows.add(row("P9", "相邻非 NONE ≥ 300ms", "runTest 虚拟时钟检查相邻执行间隔。"));
        propRows.add(row("P10", "异常隔离", "随机一项注入 SecurityException，该项 PERMISSION_DENIED，其余项不受影响。"));
        propRows.add(row("P11", "+15 分钟上限", "随机起始时长 + 任意长度 EXTEND 序列，最终 totalMs ≤ 120 * 60_000。"));
        propRows.add(row("P12", "Cancel 不写历史", "随机 ActionPlan，启动后立刻 ACTION_CANCEL，HistoryDao.count() == 0。"));
        propRows.add(row("P13", "Timeout 写一条历史", "Robolectric smoke 测试推进虚拟时钟约 2 秒，最终 state == Finished 且 count() == 1。"));
        propRows.add(row("P14", "主题持久化", "设置 themeMode 后另起 PreferencesRepository 实例仍能读到，模拟进程重启。"));
        propRows.add(row("P15", "TimerConfig 往返", "随机 TimerConfig save/load 后 durationMinutes 与 plan.normalized() 完全相等。"));
        b.add(table(headers("编号", "属性", "验证方式"), propRows));
        p(b, "上述测试位于 app/src/test/java/com/example/sleepready 下，按模块分包；属性测试基于自定义生成器在 ≥100 次迭代中验证。属性测试相比传统 example-based 测试的优势：可以批量探索边界用例（如随机长度的 EXTEND 序列、随机注入异常的项），更容易发现实现中遗漏的边界。");

        h2(b, "5.4 真机功能验证");
        p(b, "人工测试覆盖以下关键路径，每次发布前完整跑一遍：");
        List<List<String>> tcRows = new ArrayList<>();
        tcRows.add(row("TC-01", "应用首次启动", "冷启动 < 1.5s，进入印记板块；底部导航 5 个图标显示正确；权限引导 OnboardingDialog 弹出。"));
        tcRows.add(row("TC-02", "印记创建与农历显示", "创建一条「腊月廿三」的生日，列表显示「农历 廿三」；切换公历再切回农历，时间字段保持一致。"));
        tcRows.add(row("TC-03", "印记日期分组", "依次创建今天、明天、下周、下月条目；切换日期范围 chips 后列表正确分组与过滤。"));
        tcRows.add(row("TC-04", "心动多分类与图片", "创建一条同时勾选「想学」「想做」的条目，附 2 张图片；列表显示首图缩略；编辑弹窗中删除一张再加一张，保存后总数 = 2。"));
        tcRows.add(row("TC-05", "心动右滑切换状态", "右滑条目，按提示选择「在路上」；状态点颜色立即变蓝；再左滑触发归档，5 秒内点撤销恢复。"));
        tcRows.add(row("TC-06", "入梦倒计时", "启动一套 1 分钟方案，息屏；1 分钟后自动响铃执行；解锁后通知中心显示「X 项已自动完成」。"));
        tcRows.add(row("TC-07", "入梦 +15 分钟", "启动方案后连续点 +15 分钟 8 次，应在第 7 或 8 次触顶弹 Snackbar。"));
        tcRows.add(row("TC-08", "入梦取消", "启动后立刻按取消，倒计时立即结束；查看历史记录列表，没有新增条目（P12）。"));
        tcRows.add(row("TC-09", "入梦预设排序", "长按方案选择「置顶」，该条移到列表最上并显示金色 PushPin 图标；再长按「取消置顶」恢复。"));
        tcRows.add(row("TC-10", "起床后自动恢复", "设置 1 分钟方案 + 当天「+2 分钟」自动恢复；倒计时归零后看到对应开关被改变；2 分钟后看到反向操作执行完毕。"));
        tcRows.add(row("TC-11", "时光图片放大", "详情页点击图片进入全屏；左右滑动翻页；双指捏合放大到 5×；双击复位；单击关闭。"));
        tcRows.add(row("TC-12", "时光左滑删除", "列表条目右滑显示删除；点击删除弹出第一次确认；点确定弹出第二次「不可恢复」确认；两次确认后才真正删除。"));
        tcRows.add(row("TC-13", "拾句随机", "收录 5 首诗，点击 TopAppBar 的随机按钮，进入详情页；持续点击 10 次都能正常切换。"));
        tcRows.add(row("TC-14", "全局搜索", "设置页搜索图标 → 输入「春」，结果按板块分组显示，匹配字「春」高亮淡黄背景；点击结果跳转到对应板块。"));
        tcRows.add(row("TC-15", "数据统计", "设置页 → 数据统计；总条目数与各板块条目数加和一致；最近 7 天柱状图最高柱对应今天的新增数。"));
        tcRows.add(row("TC-16", "时光数据导出", "设置页 → 导出时光数据；下载目录看到 sleepready_moments_<时间>.zip；解压后图片完整。"));
        tcRows.add(row("TC-17", "左滑/系统返回", "在印记编辑器、时光详情页、入梦预设编辑器中按系统返回手势，回到上一层而不是首页。"));
        tcRows.add(row("TC-18", "主题切换", "切换深色/浅色/跟随系统三档，重启应用后偏好保持。"));
        tcRows.add(row("TC-19", "配色切换", "依次切换晨曦/林间/墨夜，所有页面 primary 色立即更新。"));
        tcRows.add(row("TC-20", "桌面 Widget", "添加 Widget 到桌面，点击「一键开始上次模式」直接进入倒计时页；上次未配置时跳转入梦列表。"));
        b.add(table(headers("编号", "场景", "预期结果"), tcRows));

        h2(b, "5.5 已知限制");
        p(b, "在测试过程中，以下若干限制是 Android 系统级行为而非应用 bug，需要用户主动配合：");
        bul(b, "Android 10+ 禁用第三方应用直接开关 Wi-Fi 与移动数据，应用通过跳转设置页 + 通知提示的方式引导，最终需用户点击。");
        bul(b, "锁屏且有密码状态下，无障碍服务无法注入触摸事件 —— 这是 Android 安全机制，应用通过分层执行策略把 B 类开关延后到 USER_PRESENT 解锁广播触发。");
        bul(b, "部分国产 ROM 会延迟或拦截 AlarmManager 唤醒，建议用户把应用加入电池白名单。");
        bul(b, "目标 sdk 34+ 上 Bluetooth API 弹出系统对话框要求用户确认，无法静默切换。");
        b.add(c("PB", null));

        // ---------- 第六章 部署与运行 ----------
        h1(b, "第六章 部署与运行");

        h2(b, "6.1 构建步骤");
        p(b, "本地开发环境只需要安装 Android Studio Hedgehog 2023.1.1 或更新（自带 JDK 17 与 SDK 34）。Windows 用户由于 GRADLE_OPTS 环境变量可能被外部脚本污染，需要在 PowerShell 中先清空：");
        code(b, "$env:GRADLE_OPTS = ''\n.\\gradlew.bat assembleDebug");
        p(b, "项目根目录提供了一键脚本 _build.ps1，封装了清空环境变量与日志重定向：");
        code(b,
            "# _build.ps1 摘要\n" +
            "$env:GRADLE_OPTS = ''\n" +
            "$env:JAVA_TOOL_OPTIONS = ''\n" +
            "& .\\gradlew.bat --no-daemon assembleDebug *>&1 | Out-File build_log.txt\n" +
            "\"EXITCODE=$LASTEXITCODE\" | Out-File build_exit.txt");
        p(b, "构建产物：app/build/outputs/apk/debug/app-debug.apk，约 18.99 MB。");

        h2(b, "6.2 安装与首次运行");
        bul(b, "将 APK 拷贝到手机或通过 adb install -r app-debug.apk 安装。");
        bul(b, "首次启动会显示 OnboardingDialog 列出本应用要申请的权限及理由，包括 POST_NOTIFICATIONS（API 33+）、CAMERA（手电筒）、BLUETOOTH_CONNECT（API 31+）、SCHEDULE_EXACT_ALARM 等。");
        bul(b, "关闭引导后即可正常使用；勿扰、屏幕录制等需要在权限矩阵中点击对应行后跳转特殊设置页授予。");

        h2(b, "6.3 数据存储位置");
        List<List<String>> storeRows = new ArrayList<>();
        storeRows.add(row("Room 数据库", "/data/data/com.example.sleepready/databases/sleep_ready.db", "7 张表，约 < 1 MB"));
        storeRows.add(row("UI 偏好", "/data/data/com.example.sleepready/shared_prefs/ui_prefs.xml", "主题、配色、自动化策略、Onboarding 标志"));
        storeRows.add(row("运行态偏好", "/data/data/com.example.sleepready/shared_prefs/runtime_prefs.xml", "当前运行 preset id、pending 引导计划、自动恢复 plan"));
        storeRows.add(row("时光图片", "/data/data/com.example.sleepready/files/moments/<uuid>.jpg", "应用私有，卸载即清空"));
        storeRows.add(row("心动图片", "/data/data/com.example.sleepready/files/knowledge/<uuid>.jpg", "同上"));
        storeRows.add(row("时光备份", "Download/sleepready_moments_*.zip", "用户主动导出，API 29+ MediaStore"));
        b.add(table(headers("数据", "路径", "说明"), storeRows));

        h2(b, "6.4 反馈与升级");
        p(b, "应用为单机离线，没有内嵌反馈通道。版本升级通过侧载新 APK；DB version 升级时使用 fallbackToDestructiveMigration() —— 在公测前会替换为完整 Migration 链以保留用户数据。");
        b.add(c("PB", null));

        // ---------- 第七章 心得体会 ----------
        h1(b, "第七章 心得体会");

        h2(b, "7.1 关于「克制」的产品观");
        p(b, "从最早的「睡前定时」单一功能，到逐步扩展为五板块的全功能记录工具，本项目最大的考验不是技术难度，而是「什么不该做」。每加一个功能，都要先问三个问题：它和睡前的仪式感有关吗？它会不会让用户每次进入应用都额外多想一步？它能不能用现有结构完成而不引入新的概念？");
        p(b, "心动板块的状态机从最初设计的「未做 / 暂停 / 完成」改为「想去做 / 在路上 / 已抵达」，技术上没有任何变化，但用户感受立刻不一样了。这让我意识到：好的产品文案与好的代码同等重要，恰当的命名能省下很多解释。");
        p(b, "入梦板块的 ActionPlan 序列化与执行引擎抽象出 13 项开关、3 种动作（NONE / TURN_ON / TURN_OFF）、2 种执行层级（SILENT / GUIDED），用极少的概念覆盖了 13 个完全不同的系统服务调用。这种「最小可表达模型」思维让后续添加新开关（比如未来的省电模式、护眼模式）成本极低，也让分层执行策略与起床后自动恢复这两个看起来跨度很大的功能能复用同一套 ExecutionEngine。");

        h2(b, "7.2 关于 Compose 与状态管理");
        p(b, "本项目是我第一次把 Compose 用到一个完整应用的规模。最深的体会是 collectAsStateWithLifecycle、StateFlow、SharedFlow 三件套配合 ViewModel 比传统 LiveData 更清晰：单向数据流没有任何 mutable observer 的回调嵌套，所有 UI 状态从 Flow 而来，所有用户交互通过 ViewModel 方法回流到 Repository。");
        p(b, "另一个发现是 Compose Navigation 的 single Activity + NavHost 结构非常适合用 sealed interface 做「页面内 overlay」状态机：印记板块的列表 / TypePicker / Editor 三态用 EditorOverlay 三个 data object/data class 表示，BackHandler 拦截系统返回手势按状态机回退一层而不是退出 NavHost。这种模式对深层模态流（比如时光的列表 / 编辑 / 详情）非常友好，避免了过多的 NavGraph 嵌套。");

        h2(b, "7.3 关于 Java 与 Kotlin 互操作");
        p(b, "后期把农历换算、搜索引擎、数据统计、拾句仓库改写为 Java，最大的挑战是 Kotlin suspend 函数在 JVM 字节码层是带 Continuation 的特殊方法，Java 直接调用非常麻烦。最终采用「桥接器」模式：在 Kotlin 侧写一个非常薄的类（VerseDaoBridge / SearchDaoBridge），用 runBlocking 把 suspend DAO 包成阻塞同步方法；Java 仓库只看到普通的 List 返回值。这种分工明确、各自取长的搭配比纯 Kotlin 实现更利于他人阅读，也强迫我把核心算法的纯函数性写得更干净。");
        p(b, "一个具体的收益：Java 的 LunarCalendar 用 final class + 大段静态常量表表达 1900-2100 的农历信息，配合静态方法形成一目了然的纯函数库；同样的代码用 Kotlin object 写可读性反而稍逊。把领域分给最适合表达它的语言，远比强求统一更划算。");

        h2(b, "7.4 关于 Android 系统的边界");
        p(b, "入梦板块的开发让我直面 Android 自 10 起逐步收紧的硬件控制权限：Wi-Fi 不能直接开关、飞行模式无 API、移动数据无 API、定位无 API、NFC 无 API。最初的实现尝试通过 root + reflect 暴力调用 hidden API，发现兼容性灾难，立刻意识到这是一条死路。");
        p(b, "最终选择的「分层 + 引导」策略其实是一种妥协：能静默的项立即执行（A 类），不能静默的项延后到用户解锁瞬间通过无障碍服务回放（B 类）。即便如此，还是要直面无障碍服务在锁屏状态下无法注入触摸的硬限制。这次实践让我对「Android 系统不再是早期的开放沙盒」有了切身体会，也提醒未来设计自动化类应用时，要把用户参与点放进交互流程而不是奢望「全自动」。");

        h2(b, "7.5 关于属性测试");
        p(b, "在 design.md 阶段抽出 P1-P15 共 15 条不变量并把它们作为后续测试任务的明确目标，是这个项目对我影响最大的方法论之一。传统的 example-based 单元测试只能覆盖手工想到的几个用例，而属性测试通过随机生成器在 ≥ 100 次迭代里探索边界，能发现实现中潜在的非平凡 bug。");
        p(b, "一个具体的发现：原本以为「+15 分钟上限 120 分钟」很简单，写好属性测试后发现，如果用户先 +15 三次到达 105，再快速 +15 两次（在 ticker 还没更新 totalMs 之前），就可能观测到 totalMs > 120。修复方式是把上限检查放在 totalMs 加之前而不是之后，并用 AtomicLong 替代 var。这种 race condition 是手写示例测试很难覆盖的。");

        h2(b, "7.6 关于工程实践与协作");
        p(b, "本项目从立项到完成历时若干阶段，期间所有的设计决策、踩坑记录、修改清单都被组织成 .kiro/specs/sleep-ready/ 下的 requirements.md / design.md / tasks.md / RESUME_NOTES.md 四份文档。每次大改都先列出修改清单等用户确认再动手；每次修改完都跑 assembleDebug 验证编译；每次遇到 race / 兼容性问题都在 RESUME_NOTES 中留下背景以便回顾。这种「写下来再做」的纪律虽然慢，但避免了反复返工，也为这份报告的撰写提供了一手资料。");
        p(b, "另一个体会是「修改清单 + 用户确认」的流程比直接动手好得多。当用户面对一个长清单时往往能立刻指出「这一项不要」「那一项调整」，而不是在动手之后才发现方向错了。每一次确认都是一次廉价的代价，每一次返工都是一次昂贵的代价。");

        h2(b, "7.7 待改进项");
        bul(b, "把 fallbackToDestructiveMigration 替换为完整 Migration 链，避免发布后用户数据丢失。");
        bul(b, "补全 P1-P15 中尚未自动化的属性测试（部分仍依赖手工真机验证）。");
        bul(b, "把目前的桌面 Widget 升级为 Glance 实现，提供更丰富的状态展示。");
        bul(b, "心动板块的图片附件支持长按从 PhotoPicker 转到拍照入口，提升记录效率。");
        bul(b, "搜索引擎可以增加倒排索引把 O(n) 全表扫描降到 O(log n)，应对百条以上的数据规模。");
        bul(b, "为视障用户补充 contentDescription 与对比度审计（WCAG 2.1 AA 起步）。");

        h2(b, "7.8 结语");
        p(b, "一个看似简单的「睡前小工具」做起来牵涉到的细节远超预期：从公历农历转换到 Doze 兜底、从无障碍服务到分层执行、从 Compose 状态管理到 Java/Kotlin 混合工程。但正是这些细节让我对 Android 开发有了更立体的认知，也让「拾刻」这个产品具备了它的温度。");
        p(b, "希望这份报告不仅记录了技术过程，也传达了它背后的产品思考。把心安顿在十五分钟之内，是这个应用的本意，也是这次开发体验的最大收获。");
        b.add(c("PB", null));

        // ---------- 附录 ----------
        h1(b, "附录 A 模块代码清单");
        p(b, "本附录罗列项目中按职能划分的关键源文件，方便审阅。");
        List<List<String>> appRows = new ArrayList<>();
        appRows.add(row("入口", "MainActivity.kt / SleepReadyApp.kt"));
        appRows.add(row("DI", "di/AppContainer.kt"));
        appRows.add(row("领域模型", "domain/model/{Schedule,KnowledgeItem,Moment,VerseItem,TimerPreset,TimerConfig,ActionPlan,Enums,LunarCalendar}.kt"));
        appRows.add(row("Room 数据层", "data/db/{AppDatabase,Entities,Converters,*Dao}.kt"));
        appRows.add(row("Preferences", "data/prefs/PreferencesRepository.kt"));
        appRows.add(row("Kotlin 仓库", "repository/{KnowledgeRepository,ScheduleRepository,MomentRepository,MomentBackup,TimerPresetRepository,TimerConfigRepository,HistoryRepository,MacroRepository}.kt"));
        appRows.add(row("Java 仓库", "verse/VerseRepository.java / statistics/StatisticsRepository.java"));
        appRows.add(row("Java 算法", "lunar/LunarCalendar.java / search/SearchEngine.java / statistics/StatisticsCalculator.java / search/SearchableItem.java"));
        appRows.add(row("Java↔Kotlin 桥", "verse/VerseDaoBridge.kt / search/SearchDaoBridge.kt"));
        appRows.add(row("权限", "permission/{PermissionMatrix,PermissionManager,PermissionState}.kt"));
        appRows.add(row("前台服务", "service/{SleepReadyTimerService,ExecutionEngine,SystemActions,ScreenRecordingController,TimerStateHolder,AlarmReceiver,UnlockReceiver,RestoreReceiver}.kt"));
        appRows.add(row("通知", "service/notif/TimerNotifications.kt"));
        appRows.add(row("日程提醒", "service/schedule/{ScheduleReminderScheduler,ScheduleReminderReceiver}.kt"));
        appRows.add(row("宏录制", "service/macro/{MacroAccessibilityService,MacroPlayer,MacroModels}.kt"));
        appRows.add(row("印记 UI", "ui/schedule/{ScheduleScreen,ScheduleViewModel,ScheduleSummaries}.kt + components/"));
        appRows.add(row("心动 UI", "ui/inbox/{KnowledgeInboxScreen,KnowledgeInboxViewModel}.kt + components/"));
        appRows.add(row("入梦 UI", "ui/timer/{TimerPresetListScreen,TimerPresetListViewModel,TimerPresetEditorScreen,CountdownScreen,CountdownViewModel}.kt"));
        appRows.add(row("时光 UI", "ui/moment/{MomentScreen,MomentViewModel}.kt + components/{MomentCard,MomentDetailScreen,MomentEditorScreen,ImageViewerDialog,MomentSwipeRow}.kt"));
        appRows.add(row("拾句 UI", "ui/verse/{VerseScreen,VerseViewModel,VerseDetailScreen,VerseEditorScreen}.kt"));
        appRows.add(row("搜索 UI", "ui/search/{SearchScreen,SearchViewModel}.kt"));
        appRows.add(row("统计 UI", "ui/statistics/{StatisticsScreen,StatisticsViewModel}.kt"));
        appRows.add(row("历史 UI", "ui/history/{HistoryScreen,HistoryViewModel}.kt"));
        appRows.add(row("设置 UI", "ui/settings/{SettingsScreen,SettingsViewModel}.kt"));
        appRows.add(row("主题", "ui/theme/{Color,Type,Shape,SleepReadyTheme}.kt"));
        appRows.add(row("通用组件", "ui/common/{WheelPicker,DateTimeWheelSheet}.kt"));
        appRows.add(row("导航", "ui/nav/{Routes,SleepReadyNavGraph}.kt"));
        appRows.add(row("Widget", "widget/{SleepReadyWidgetProvider,WidgetReceiver}.kt"));
        b.add(table(headers("类别", "主要文件"), appRows));
        b.add(c("PB", null));

        return b;
    }

    static List<String> headers(String... arr) {
        List<String> h = new ArrayList<>();
        for (String s : arr) h.add(s);
        return h;
    }
    static List<String> row(String... arr) {
        List<String> r = new ArrayList<>();
        for (String s : arr) r.add(s);
        return r;
    }
    static Block table(List<String> headers, List<List<String>> rows) {
        return new Block("TBL", headers, rows);
    }

    static void h1(List<Block> b, String t) { b.add(new Block("H1", t)); }
    static void h2(List<Block> b, String t) { b.add(new Block("H2", t)); }
    static void h3(List<Block> b, String t) { b.add(new Block("H3", t)); }
    static void p(List<Block> b, String t)  { b.add(new Block("P", t)); }
    static void bul(List<Block> b, String t){ b.add(new Block("BUL", t)); }
    static void code(List<Block> b, String t){ b.add(new Block("CODE", t)); }
    static Block c(String type, String t)   { return new Block(type, t); }
}

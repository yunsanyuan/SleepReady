package com.example.sleepready.ui.nav

/**
 * 五大顶级板块 + 二级路由。底部导航栏使用前 5 个 root 路由。
 */
object Routes {
    // 顶级
    const val SCHEDULE = "schedule"
    const val EXPLORE = "explore"
    const val SLEEP_TIMER = "sleep_timer"
    const val MOMENT = "moment"
    const val VERSE = "verse"

    // 二级
    const val TIMER_PRESET_NEW = "timer/preset/new"
    const val TIMER_PRESET_EDIT = "timer/preset/edit/{id}"
    const val TIMER_COUNTDOWN = "timer/countdown"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val STATISTICS = "statistics"
    const val VERSE_DETAIL = "verse/detail/{id}"
    const val VERSE_NEW = "verse/new"
    const val VERSE_EDIT = "verse/edit/{id}"

    fun timerPresetEdit(id: Long): String = "timer/preset/edit/$id"
    fun verseDetail(id: Long): String = "verse/detail/$id"
    fun verseEdit(id: Long): String = "verse/edit/$id"
}

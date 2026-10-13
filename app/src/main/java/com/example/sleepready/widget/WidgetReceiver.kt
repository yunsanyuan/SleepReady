package com.example.sleepready.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sleepready.ui.nav.Routes

/**
 * Handles taps on [SleepReadyWidgetProvider] button.
 *
 * v3：不再尝试在 receiver 主线程读 Room（避免 ANR）。点击 widget 一律打开 App
 * 的「入梦」预设列表页（[Routes.SLEEP_TIMER]），由用户在列表里点「启动」。
 *
 * 这样消除了 BroadcastReceiver 上的 `runBlocking` 同步 IO，也让 widget 行为
 * 跟主界面一致（同一个 preset 列表入口）。
 */
class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SleepReadyWidgetProvider.ACTION_QUICK_START) return

        val mainActivityClass = runCatching {
            Class.forName(MAIN_ACTIVITY_CLASS_NAME)
        }.getOrNull() ?: return

        val activityIntent = Intent(context, mainActivityClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_NAV, Routes.SLEEP_TIMER)
        }
        context.startActivity(activityIntent)
    }

    companion object {
        const val EXTRA_NAV = "com.example.sleepready.EXTRA_NAV"
        private const val MAIN_ACTIVITY_CLASS_NAME = "com.example.sleepready.MainActivity"
    }
}

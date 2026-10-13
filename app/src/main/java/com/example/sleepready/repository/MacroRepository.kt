package com.example.sleepready.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.service.macro.Macro
import kotlinx.serialization.json.Json

/**
 * 宏录制持久化（每个 [SwitchItem] 一条 [Macro]，用 SharedPreferences 存 JSON）。
 *
 * 用 prefs 而非 Room 的理由：宏数量极少（≤ 6），结构简单，不需要查询能力。
 */
interface MacroRepository {
    fun getMacro(item: SwitchItem): Macro?
    fun saveMacro(macro: Macro)
    fun deleteMacro(item: SwitchItem)
    fun listAll(): List<Macro>
}

class MacroRepositoryImpl(context: Context) : MacroRepository {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getMacro(item: SwitchItem): Macro? {
        val raw = prefs.getString(keyFor(item), null) ?: return null
        return runCatching { json.decodeFromString(Macro.serializer(), raw) }.getOrNull()
    }

    override fun saveMacro(macro: Macro) {
        val payload = json.encodeToString(Macro.serializer(), macro)
        prefs.edit { putString(keyFor(macro.switchItem), payload) }
    }

    override fun deleteMacro(item: SwitchItem) {
        prefs.edit { remove(keyFor(item)) }
    }

    override fun listAll(): List<Macro> = SwitchItem.values().mapNotNull { getMacro(it) }

    private fun keyFor(item: SwitchItem): String = "macro_${item.name}"

    companion object {
        private const val PREFS_NAME = "sleep_ready_macros"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

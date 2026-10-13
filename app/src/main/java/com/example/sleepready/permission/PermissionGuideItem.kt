package com.example.sleepready.permission

enum class ActionType {
    REQUEST_RUNTIME,
    GOTO_SETTINGS,
    GOTO_ROM_SETTINGS
}

data class PermissionGuideItem(
    val key: String,
    val title: String,
    val description: String,
    val priority: Int,
    val actionType: ActionType,
    val runtimePermission: String? = null,
    val settingsIntent: android.content.Intent? = null
)
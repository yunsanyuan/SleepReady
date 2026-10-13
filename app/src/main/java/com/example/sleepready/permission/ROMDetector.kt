package com.example.sleepready.permission

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

enum class ROMType {
    STOCK, MIUI, EMUI, HARMONYOS, COLOROS, ORIGINOS, UNKNOWN
}

object ROMDetector {

    private const val TAG = "ROMDetector"

    fun detectROM(): ROMType {
        if (isHarmonyOS()) return ROMType.HARMONYOS
        if (isMIUI()) return ROMType.MIUI
        if (isEMUI()) return ROMType.EMUI
        if (isColorOS()) return ROMType.COLOROS
        if (isOriginOS()) return ROMType.ORIGINOS
        return ROMType.STOCK
    }

    fun detectMissingPermissions(context: Context): List<ROMPermissionStatus> {
        return when (detectROM()) {
            ROMType.MIUI -> detectMIUIPermissions(context)
            ROMType.EMUI -> detectEMUIPermissions(context)
            ROMType.HARMONYOS -> detectHarmonyOSPermissions(context)
            ROMType.COLOROS -> detectColorOSPermissions(context)
            ROMType.ORIGINOS -> detectOriginOSPermissions(context)
            else -> emptyList()
        }
    }

    fun toGuideItems(permissions: List<ROMPermissionStatus>): List<PermissionGuideItem> =
        permissions.map { perm ->
            PermissionGuideItem(
                key = perm.permissionKey,
                title = perm.type,
                description = "在${perm.type}系统设置中开启此权限，否则提醒可能无法正常触发",
                priority = 1,
                actionType = ActionType.GOTO_ROM_SETTINGS,
                settingsIntent = perm.settingsIntent
            )
        }

    private fun isMIUI(): Boolean = try {
        !System.getProperty("ro.miui.ui.version.name").isNullOrBlank()
    } catch (_: Throwable) { false }

    private fun isHarmonyOS(): Boolean = try {
        !System.getProperty("ro.harmony.version").isNullOrBlank()
    } catch (_: Throwable) { false }

    private fun isEMUI(): Boolean = try {
        !System.getProperty("ro.build.version.emui").isNullOrBlank()
    } catch (_: Throwable) { false }

    private fun isColorOS(): Boolean = try {
        !System.getProperty("ro.build.version.oplusrom").isNullOrBlank()
    } catch (_: Throwable) { false }

    private fun isOriginOS(): Boolean = try {
        !System.getProperty("ro.vivo.os.version").isNullOrBlank()
    } catch (_: Throwable) { false }

    private fun detectMIUIPermissions(context: Context): List<ROMPermissionStatus> = listOf(
        ROMPermissionStatus(
            type = "MIUI",
            permissionKey = "miui_autostart",
            isGranted = false,
            settingsIntent = resolveROMIntent(
                context,
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                fallback = buildAppDetailsIntent(context)
            )
        ),
        ROMPermissionStatus(
            type = "MIUI",
            permissionKey = "miui_background_popup",
            isGranted = false,
            settingsIntent = resolveROMIntent(
                context,
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"),
                fallback = buildAppDetailsIntent(context)
            )
        )
    )

    private fun detectEMUIPermissions(context: Context): List<ROMPermissionStatus> = listOf(
        ROMPermissionStatus(
            type = "EMUI",
            permissionKey = "emui_autostart",
            isGranted = false,
            settingsIntent = resolveROMIntent(
                context,
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                fallback = buildAppDetailsIntent(context)
            )
        )
    )

    private fun detectHarmonyOSPermissions(context: Context): List<ROMPermissionStatus> = listOf(
        ROMPermissionStatus(
            type = "HarmonyOS",
            permissionKey = "harmonyos_autostart",
            isGranted = false,
            settingsIntent = resolveROMIntent(
                context,
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                fallback = buildAppDetailsIntent(context)
            )
        )
    )

    private fun detectColorOSPermissions(context: Context): List<ROMPermissionStatus> = listOf(
        ROMPermissionStatus(
            type = "ColorOS",
            permissionKey = "coloros_autostart",
            isGranted = false,
            settingsIntent = resolveROMIntent(
                context,
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                fallback = buildAppDetailsIntent(context)
            )
        )
    )

    private fun detectOriginOSPermissions(context: Context): List<ROMPermissionStatus> = listOf(
        ROMPermissionStatus(
            type = "OriginOS",
            permissionKey = "originos_autostart",
            isGranted = false,
            settingsIntent = resolveROMIntent(
                context,
                ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"),
                fallback = buildAppDetailsIntent(context)
            )
        )
    )

    private fun resolveROMIntent(context: Context, component: ComponentName, fallback: Intent): Intent {
        val intent = Intent().apply {
            setComponent(component)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("pkg", context.packageName)
        }
        return try {
            if (intent.resolveActivity(context.packageManager) != null) intent else fallback
        } catch (_: Throwable) { fallback }
    }

    private fun buildAppDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
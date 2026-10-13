package com.example.sleepready.service.schedule

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.sleepready.R

/**
 * 闹钟铃声前台服务。收到 [ScheduleReminderReceiver] 转发的 [ACTION_RING] 后：
 *  1. startForeground，持有高优先级通知（含「关闭闹钟」action）
 *  2. MediaPlayer 循环播放 res/raw/alarm_sound.mp3；该资源不存在则降级到
 *     [RingtoneManager.TYPE_ALARM] 系统默认闹钟铃声
 *  3. 持续震动
 *  4. PARTIAL_WAKE_LOCK 防止 CPU 进入 deep sleep（最多 5 分钟自动释放）
 *
 * 关闭路径：[ACTION_STOP] 通过通知 action 或 AlarmRingActivity 触发，
 * 停止播放、释放 wake lock、stopSelf。
 */
class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentScheduleId: Long = -1L
    private var currentTitle: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> handleRing(intent)
            ACTION_STOP -> handleStop()
            else -> handleStop()
        }
        return START_NOT_STICKY
    }

    private fun handleRing(intent: Intent) {
        currentScheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        currentTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        ensureChannel(this)
        startForegroundCompat(buildNotification(currentTitle))
        acquireWakeLock()
        startPlayback()
        startVibration()
    }

    private fun handleStop() {
        stopPlayback()
        stopVibration()
        releaseWakeLock()
        if (currentScheduleId >= 0) {
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                ?.cancel(notifId(currentScheduleId))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                notifId(currentScheduleId),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(notifId(currentScheduleId), notification)
        }
    }

    private fun startPlayback() {
        stopPlayback()
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = true
        }
        // 优先 res/raw/alarm_sound；缺失则降级为系统默认 ALARM
        val rawId = resources.getIdentifier("alarm_sound", "raw", packageName)
        try {
            if (rawId != 0) {
                resources.openRawResourceFd(rawId)?.use { afd ->
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            } else {
                val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: run { player.release(); return }
                player.setDataSource(this, uri)
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (t: Throwable) {
            try { player.release() } catch (_: Throwable) {}
            mediaPlayer = null
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
        } catch (_: Throwable) {}
        try { mediaPlayer?.release() } catch (_: Throwable) {}
        mediaPlayer = null
    }

    private fun startVibration() {
        val v = (getSystemService(VIBRATOR_SERVICE) as? Vibrator) ?: return
        vibrator = v
        val pattern = longArrayOf(0, 600, 400, 600, 400, 2000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // repeat=0 表示从 pattern[0] 开始循环，实现持续震动
            v.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        try { vibrator?.cancel() } catch (_: Throwable) {}
        vibrator = null
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        val wl = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "sleepready:AlarmRing"
        )
        wl.setReferenceCounted(false)
        wl.acquire(WAKE_LOCK_TIMEOUT_MS)
        wakeLock = wl
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {}
        wakeLock = null
    }

    private fun buildNotification(title: String): Notification {
        val stopIntent = Intent(this, AlarmRingService::class.java).setAction(ACTION_STOP)
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.alarm_ring_title))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.alarm_ring_stop),
                stopPi
            )

        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FULL_SCREEN_INTENT) == PackageManager.PERMISSION_GRANTED
        } else true

        if (canUseFullScreen) {
            val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmRingActivity.EXTRA_TITLE, title)
            }
            val fullScreenPi = PendingIntent.getActivity(
                this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPi, true)
            builder.setContentIntent(fullScreenPi)
        } else {
            Log.w(TAG, "USE_FULL_SCREEN_INTENT not granted, skip fullScreenIntent")
        }

        return builder.build()
    }

    override fun onDestroy() {
        stopPlayback()
        stopVibration()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlarmRingService"
        const val ACTION_RING = "com.example.sleepready.alarm.RING"
        const val ACTION_STOP = "com.example.sleepready.alarm.STOP"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_TITLE = "title"
        const val CHANNEL_ID = "channel_alarm_ring"
        private const val WAKE_LOCK_TIMEOUT_MS = 5L * 60 * 1000

        fun notifId(scheduleId: Long): Int =
            (scheduleId.coerceAtLeast(0L) % Int.MAX_VALUE).toInt() + 90000

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "闹钟提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "印记的闹钟铃声提醒，会在锁屏全屏显示"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400)
                setSound(
                    null,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            }
            nm.createNotificationChannel(channel)
        }

        fun start(context: Context, scheduleId: Long, title: String) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_RING
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(EXTRA_TITLE, title)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlarmRingService::class.java).setAction(ACTION_STOP)
            try { context.startService(intent) } catch (_: Throwable) {}
        }
    }
}

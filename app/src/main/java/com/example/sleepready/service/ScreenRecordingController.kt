package com.example.sleepready.service

import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import com.example.sleepready.domain.model.ExecutionEntry
import com.example.sleepready.domain.model.ExecutionResult
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import java.util.concurrent.atomic.AtomicReference

/**
 * Process-level holder for an active [MediaProjection] so the foreground service
 * can stop a screen recording when the countdown ends.
 *
 * Note: Although this is implemented as an `object` for thread-safety, it is owned
 * by `AppContainer` (see DI task 8.1) — callers should retrieve it as
 * `appContainer.screenRecordingController` rather than referencing it directly.
 *
 * Token lifecycle:
 *  - MainActivity gets a [MediaProjection] via the standard
 *    `MediaProjectionManager.createScreenCaptureIntent()` consent flow and calls [attach].
 *  - When the system or the user stops projection, the registered [MediaProjection.Callback]
 *    clears the reference automatically.
 *  - [stopIfHeld] is invoked by ExecutionEngine; if no token is held it returns a
 *    REQUIRES_USER_ACTION entry (Requirement 15.8) instead of throwing.
 */
class ScreenRecordingController {

    private val ref: AtomicReference<MediaProjection?> = AtomicReference(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Attach a freshly granted MediaProjection token. */
    fun attach(projection: MediaProjection) {
        // Replace any previous token — clean up the old one first.
        ref.getAndSet(projection)?.let { runCatching { it.stop() } }
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                ref.compareAndSet(projection, null)
            }
        }, mainHandler)
    }

    /** Drop the current token without invoking stop() — used when the activity is destroyed. */
    fun detach() {
        ref.set(null)
    }

    /**
     * Stops the active screen recording if a token is held. Never throws —
     * exceptions from [MediaProjection.stop] are converted to FAILED entries.
     * Backs Property P10.
     */
    fun stopIfHeld(): ExecutionEntry {
        val projection = ref.getAndSet(null)
        return if (projection == null) {
            ExecutionEntry(
                switchItem = SwitchItem.SCREEN_RECORDING,
                action = SwitchAction.TURN_OFF,
                result = ExecutionResult.REQUIRES_USER_ACTION,
                message = "请手动停止屏幕录制"
            )
        } else {
            try {
                projection.stop()
                ExecutionEntry(
                    switchItem = SwitchItem.SCREEN_RECORDING,
                    action = SwitchAction.TURN_OFF,
                    result = ExecutionResult.SUCCESS
                )
            } catch (t: Throwable) {
                ExecutionEntry(
                    switchItem = SwitchItem.SCREEN_RECORDING,
                    action = SwitchAction.TURN_OFF,
                    result = ExecutionResult.FAILED,
                    message = t.message ?: t::class.simpleName
                )
            }
        }
    }
}

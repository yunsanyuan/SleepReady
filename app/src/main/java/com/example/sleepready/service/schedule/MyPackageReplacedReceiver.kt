package com.example.sleepready.service.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyPackageReplacedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MyPackageReplacedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Log.d(TAG, "MY_PACKAGE_REPLACED received, rescheduling alarms")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AlarmRescheduleHelper.rescheduleAll(context)
            } catch (t: Throwable) {
                Log.e(TAG, "rescheduleAll failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
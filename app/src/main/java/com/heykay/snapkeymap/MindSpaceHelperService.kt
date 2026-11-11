package com.heykay.snapkeymap

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo

class MindSpaceHelperService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    // The handler carries only this poll, so clearing it cancels nothing else.
    private val poll = MindSpacePoll(
        schedule = { delayMs, task -> handler.postDelayed({ task() }, delayMs) },
        cancelScheduled = { handler.removeCallbacksAndMessages(null) },
        now = SystemClock::uptimeMillis,
        click = ::clickSnapToMindSpace,
    )

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    fun sendScreenshotToMindSpace(onUnavailable: () -> Unit) {
        poll.cancel()
        if (!performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)) {
            onUnavailable()
            return
        }
        poll.start(onUnavailable)
    }

    private fun clickSnapToMindSpace(): MindSpaceClick {
        for (window in windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() != "com.oplus.screenshot") continue
            val button = root.findAccessibilityNodeInfosByViewId(
                "com.oplus.screenshot:id/collection_icon_container",
            ).firstOrNull { it.isEnabled } ?: continue
            val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return if (clicked) MindSpaceClick.CLICKED else MindSpaceClick.FAILED
        }
        return MindSpaceClick.NOT_READY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        clear()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clear()
        super.onDestroy()
    }

    private fun clear() {
        poll.cancel()
        instance = null
    }

    companion object {
        @Volatile
        var instance: MindSpaceHelperService? = null
            private set

        fun isEnabled(context: Context): Boolean = context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val service = it.resolveInfo.serviceInfo
                ComponentName(service.packageName, service.name) == ComponentName(context, MindSpaceHelperService::class.java)
            }
    }
}

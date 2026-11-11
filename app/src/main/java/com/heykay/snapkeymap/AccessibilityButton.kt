package com.heykay.snapkeymap

import android.accessibilityservice.AccessibilityService

/** Do not dispatch an action the device does not currently expose. Called on Android 12+. */
internal fun performAccessibilityButton(
    availableActions: List<Int>,
    performAction: (Int) -> Boolean,
): Boolean {
    val id = AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_BUTTON
    return id in availableActions && performAction(id)
}

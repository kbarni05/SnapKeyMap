package com.heykay.snapkeymap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityButtonActionTest {
    @Test
    fun availableButtonDispatchesExactlyTheAccessibilityButtonAction() {
        val dispatched = mutableListOf<Int>()
        assertTrue(performAccessibilityButton(listOf(1, 11, 13)) {
            dispatched += it
            true
        })
        assertEquals(listOf(11), dispatched)
    }

    @Test
    fun unavailableButtonDoesNotTriggerTheDifferentVolumeKeyShortcut() {
        val dispatched = mutableListOf<Int>()
        assertFalse(performAccessibilityButton(listOf(1, 12, 13)) {
            dispatched += it
            true
        })
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun emptySystemActionsDoNotDispatchAnything() {
        val dispatched = mutableListOf<Int>()
        assertFalse(performAccessibilityButton(emptyList()) {
            dispatched += it
            true
        })
        assertTrue(dispatched.isEmpty())
    }

    @Test
    fun rejectedButtonActionReportsFailureWithoutRetrying() {
        val dispatched = mutableListOf<Int>()
        assertFalse(performAccessibilityButton(listOf(11)) {
            dispatched += it
            false
        })
        assertEquals(listOf(11), dispatched)
    }

    @Test
    fun savedAccessibilityButtonMappingCanBeRestoredAndBackedUp() {
        val action = SnapAction.fromJson("""{"type":"device","action":"ACCESSIBILITY_BUTTON"}""")

        assertNotNull("An accessibility button mapping must not be discarded on restore", action)
        assertTrue((action as SnapAction.Device).action.needsService)
        assertEquals(Section.SCREEN, action.action.section)
        assertEquals(action, SnapAction.fromJson(action.toJson()))
    }
}

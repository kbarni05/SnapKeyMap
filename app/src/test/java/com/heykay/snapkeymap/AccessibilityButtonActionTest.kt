package com.heykay.snapkeymap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityButtonActionTest {
    @Test
    fun savedAccessibilityButtonMappingCanBeRestoredAndBackedUp() {
        val action = SnapAction.fromJson("""{"type":"device","action":"ACCESSIBILITY_BUTTON"}""")

        assertNotNull("An accessibility button mapping must not be discarded on restore", action)
        assertTrue((action as SnapAction.Device).action.needsService)
        assertEquals(Section.SCREEN, action.action.section)
        assertEquals(action, SnapAction.fromJson(action.toJson()))
    }
}

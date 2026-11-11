package com.heykay.snapkeymap

/** What one attempt to press Snap to Mind Space in the screenshot preview found. */
enum class MindSpaceClick {
    /** The button was pressed. */
    CLICKED,

    /** The preview or the button is not on screen yet. */
    NOT_READY,

    /** The button was found but pressing it failed. */
    FAILED,
}

/**
 * Polls for the screenshot preview and presses Snap to Mind Space.
 *
 * Scheduling, the clock and the click are injected, so this stays free of Android APIs.
 */
class MindSpacePoll(
    private val schedule: (delayMs: Long, task: () -> Unit) -> Unit,
    private val cancelScheduled: () -> Unit,
    private val now: () -> Long,
    private val click: () -> MindSpaceClick,
    private val deadlineMs: Long = 3000L,
    private val intervalMs: Long = 150L,
) {
    // Every request gets a token of its own so callbacks from older or cancelled requests do nothing.
    private var generation = 0
    private var deadline = 0L

    fun start(onUnavailable: () -> Unit) {
        cancelScheduled()
        generation++
        val token = generation
        deadline = now() + deadlineMs
        schedule(intervalMs) { attempt(token, onUnavailable) }
    }

    fun cancel() {
        generation++
        cancelScheduled()
    }

    private fun attempt(token: Int, onUnavailable: () -> Unit) {
        if (token != generation) return
        val outcome = runCatching { click() }.getOrElse { MindSpaceClick.FAILED }
        if (token != generation) return
        if (outcome == MindSpaceClick.CLICKED) return
        if (now() >= deadline) {
            onUnavailable()
            return
        }
        schedule(intervalMs) { attempt(token, onUnavailable) }
    }
}

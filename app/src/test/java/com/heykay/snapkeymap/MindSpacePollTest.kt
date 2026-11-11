package com.heykay.snapkeymap

import org.junit.Assert.assertEquals
import org.junit.Test

class MindSpacePollTest {
    private val clock = FakePollClock()
    private var attempts = 0
    private var unavailable = 0
    private val outcomes = mutableListOf<MindSpaceClick>()
    private var click: () -> MindSpaceClick = {
        attempts++
        outcomes.removeFirstOrNull() ?: MindSpaceClick.NOT_READY
    }

    private fun poll(deadlineMs: Long = 3000L) = MindSpacePoll(
        schedule = clock::schedule,
        cancelScheduled = clock::cancel,
        now = { clock.now },
        click = { click() },
        deadlineMs = deadlineMs,
    )

    private fun queue(vararg queued: MindSpaceClick) {
        outcomes.addAll(queued)
    }

    @Test
    fun pressesOnceTheButtonAppears() {
        queue(MindSpaceClick.NOT_READY, MindSpaceClick.CLICKED)
        val poll = poll()

        poll.start { unavailable++ }
        clock.advance(1000)

        assertEquals(2, attempts)
        assertEquals(0, unavailable)
    }

    @Test
    fun stopsPollingAfterTheClickSucceeds() {
        queue(MindSpaceClick.CLICKED)
        val poll = poll()

        poll.start { unavailable++ }
        clock.advance(10_000)

        assertEquals(1, attempts)
        assertEquals(0, unavailable)
    }

    @Test
    fun keepsRetryingAFailedClickUntilTheClickSucceeds() {
        queue(MindSpaceClick.FAILED, MindSpaceClick.FAILED, MindSpaceClick.FAILED, MindSpaceClick.CLICKED)
        val poll = poll()

        poll.start { unavailable++ }
        clock.advance(1000)

        assertEquals(4, attempts)
        assertEquals(0, unavailable)
    }

    @Test
    fun retriesAThrowingClickInsteadOfLeakingTheException() {
        click = {
            attempts++
            if (attempts < 3) throw IllegalStateException("stale node") else MindSpaceClick.CLICKED
        }
        val poll = poll()

        poll.start { unavailable++ }
        clock.advance(1000)

        assertEquals(3, attempts)
        assertEquals(0, unavailable)
    }

    @Test
    fun reportsUnavailableOnceAtTheDeadline() {
        val poll = poll()

        poll.start { unavailable++ }
        clock.advance(3000)

        assertEquals(20, attempts)
        assertEquals(1, unavailable)

        clock.advance(10_000)
        assertEquals(20, attempts)
        assertEquals(1, unavailable)
    }

    @Test
    fun cancelStopsPollingAndReporting() {
        val poll = poll()

        poll.start { unavailable++ }
        clock.advance(300)
        poll.cancel()
        clock.advance(10_000)

        assertEquals(2, attempts)
        assertEquals(0, unavailable)
    }

    @Test
    fun aNewRequestReplacesTheOlderOne() {
        val poll = poll()

        poll.start { unavailable++ }
        clock.advance(600)
        poll.start { unavailable++ }
        clock.advance(10_000)

        assertEquals(1, unavailable)
    }

    @Test
    fun staleCallbacksFromAnOlderRequestAreIgnored() {
        val poll = poll()

        poll.start { unavailable++ }
        val stale = clock.takeNextTask()
        val attemptsBeforeTheNewRequest = attempts

        poll.start { unavailable++ }
        stale()

        assertEquals(attemptsBeforeTheNewRequest, attempts)
        assertEquals(0, unavailable)
    }
}

/** Deterministic replacement for the service's Handler. */
private class FakePollClock {
    var now = 0L
        private set
    private val pending = mutableListOf<Pair<Long, () -> Unit>>()

    fun schedule(delayMs: Long, task: () -> Unit) {
        pending += now + delayMs to task
    }

    fun cancel() {
        pending.clear()
    }

    fun takeNextTask(): () -> Unit {
        val (_, task) = pending.removeAt(0)
        return task
    }

    fun advance(byMs: Long) {
        val target = now + byMs
        while (true) {
            val next = pending.minByOrNull { it.first } ?: break
            if (next.first > target) break
            pending.remove(next)
            now = next.first
            next.second()
        }
        now = target
    }
}

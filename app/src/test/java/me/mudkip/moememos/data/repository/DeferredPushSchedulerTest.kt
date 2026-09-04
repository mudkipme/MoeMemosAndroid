package me.mudkip.moememos.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeferredPushSchedulerTest {
    private val delayMillis = 2000L

    @Test
    fun schedulesWithinDelayCoalesceIntoOnePush() = runTest {
        val pushed = mutableListOf<String>()
        val scheduler = DeferredPushScheduler(this, delayMillis) { pushed.add(it) }

        repeat(5) {
            scheduler.schedule("a")
            advanceTimeBy(100)
        }
        assertEquals(emptyList<String>(), pushed)

        advanceUntilIdle()
        assertEquals(listOf("a"), pushed)
    }

    @Test
    fun flushPushesImmediatelyAndDropsPendingPush() = runTest {
        val pushed = mutableListOf<String>()
        val scheduler = DeferredPushScheduler(this, delayMillis) { pushed.add(it) }

        scheduler.schedule("a")
        advanceTimeBy(100)
        scheduler.flush("a")
        assertEquals(listOf("a"), pushed)

        advanceUntilIdle()
        assertEquals(listOf("a"), pushed)
    }

    @Test
    fun cancelBeforeDelayPushesNothing() = runTest {
        val pushed = mutableListOf<String>()
        val scheduler = DeferredPushScheduler(this, delayMillis) { pushed.add(it) }

        scheduler.schedule("a")
        advanceTimeBy(100)
        scheduler.cancel("a")

        advanceUntilIdle()
        assertEquals(emptyList<String>(), pushed)
    }

    @Test
    fun flushWithoutPendingJobStillPushes() = runTest {
        // A flush must always push: the row may still carry needsSync from a push that failed
        // after the deferred job ran, so the caller cannot know whether anything is pending.
        val pushed = mutableListOf<String>()
        val scheduler = DeferredPushScheduler(this, delayMillis) { pushed.add(it) }

        scheduler.schedule("a")
        advanceUntilIdle()
        assertEquals(listOf("a"), pushed)

        scheduler.flush("a")
        assertEquals(listOf("a", "a"), pushed)
    }

    @Test
    fun schedulesForDifferentIdentifiersPushSeparately() = runTest {
        val pushed = mutableListOf<String>()
        val scheduler = DeferredPushScheduler(this, delayMillis) { pushed.add(it) }

        scheduler.schedule("a")
        scheduler.schedule("b")

        advanceUntilIdle()
        assertEquals(listOf("a", "b"), pushed)
    }
}

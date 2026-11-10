package com.snothin.ghostsam.ui.screen

import com.snothin.ghostsam.ui.screen.s23.S23HomeAction
import com.snothin.ghostsam.ui.screen.s23.S23HomeStatus
import com.snothin.ghostsam.ui.screen.s23.resolveS23HomeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRootMonitorTest {
    private class Foreground(
        scope: CoroutineScope,
        suResults: List<Boolean>,
        moduleLoaded: () -> Boolean = { false },
    ) {
        private val updates = Channel<HomeRootEvidence>(Channel.UNLIMITED)
        private val retry = Channel<Unit>(Channel.UNLIMITED)
        var probeCount = 0
            private set
        val job: Job = scope.launch {
            monitorHomeRoot(
                probeSu = { suResults[probeCount++] },
                moduleLoaded = { moduleLoaded() },
                onEvidence = { updates.send(it) },
                awaitNextProbe = { retry.receive() },
            )
        }

        suspend fun next(): HomeRootEvidence = updates.receive()
        suspend fun retry() { retry.send(Unit) }
    }

    private fun home(evidence: HomeRootEvidence) = resolveS23HomeState(
        running = false,
        managerInstalled = true,
        suProbeDone = true,
        suAvailable = evidence.suAvailable,
        kernelLoaded = evidence.moduleLoaded,
    )

    @Test
    fun coldStartRetriesLocalSuWithoutAdbOrShizuku() = runBlocking {
        withTimeout(5_000L) {
            val foreground = Foreground(this, listOf(false, false, true))
            try {
                assertEquals(S23HomeStatus.Ready, home(foreground.next()).status)
                foreground.retry()
                assertEquals(S23HomeStatus.Ready, home(foreground.next()).status)
                foreground.retry()
                val enabled = home(foreground.next())
                assertEquals(S23HomeStatus.Enabled, enabled.status)
                assertEquals(S23HomeAction.OpenManager, enabled.action)
                foreground.job.join()
                assertEquals(3, foreground.probeCount)
            } finally {
                foreground.job.cancelAndJoin()
            }
        }
    }

    @Test
    fun reopenedLoadedSessionWaitsForGrantInsteadOfOfferingAnotherRun() = runBlocking {
        withTimeout(5_000L) {
            val foreground = Foreground(this, listOf(false, true), moduleLoaded = { true })
            try {
                val awaiting = foreground.next()
                assertTrue(awaiting.moduleLoaded)
                assertFalse(awaiting.suAvailable)
                assertEquals(S23HomeStatus.AwaitingGrant, home(awaiting).status)
                assertEquals(S23HomeAction.OpenManager, home(awaiting).action)
                foreground.retry()
                assertEquals(S23HomeStatus.Enabled, home(foreground.next()).status)
                foreground.job.join()
            } finally {
                foreground.job.cancelAndJoin()
            }
        }
    }

    @Test
    fun liveLoadingEvidenceAppearsAfterTheInitialSuFailure() = runBlocking {
        withTimeout(5_000L) {
            var loadedThisBoot = false
            val foreground = Foreground(this, listOf(false, false), moduleLoaded = { loadedThisBoot })
            try {
                assertEquals(S23HomeStatus.Ready, home(foreground.next()).status)
                loadedThisBoot = true
                foreground.retry()
                assertEquals(S23HomeStatus.AwaitingGrant, home(foreground.next()).status)
            } finally {
                foreground.job.cancelAndJoin()
            }
        }
    }

    @Test
    fun aHiddenModuleDoesNotEraseLoadingEvidenceOnTheNextPoll() = runBlocking {
        withTimeout(5_000L) {
            var moduleVisible = true
            val foreground = Foreground(this, listOf(false, false), moduleLoaded = { moduleVisible })
            try {
                assertEquals(S23HomeStatus.AwaitingGrant, home(foreground.next()).status)
                moduleVisible = false
                foreground.retry()
                assertEquals(S23HomeStatus.AwaitingGrant, home(foreground.next()).status)
            } finally {
                foreground.job.cancelAndJoin()
            }
        }
    }

    @Test
    fun foregroundReentryChecksTheCurrentGrantInsteadOfRestoringAnOldSuccess() = runBlocking {
        withTimeout(5_000L) {
            val before = Foreground(this, listOf(true))
            assertEquals(S23HomeStatus.Enabled, home(before.next()).status)
            before.job.join()
            val after = Foreground(this, listOf(false), moduleLoaded = { true })
            try {
                assertEquals(S23HomeStatus.AwaitingGrant, home(after.next()).status)
                assertEquals(1, after.probeCount)
            } finally {
                after.job.cancelAndJoin()
            }
        }
    }

    @Test
    fun leavingTheForegroundCancelsRetriesAndReturningStartsAFreshProbe() = runBlocking {
        withTimeout(5_000L) {
            val before = Foreground(this, listOf(false))
            assertEquals(S23HomeStatus.Ready, home(before.next()).status)
            before.job.cancelAndJoin()
            assertEquals(1, before.probeCount)
            val after = Foreground(this, listOf(true))
            assertEquals(S23HomeStatus.Enabled, home(after.next()).status)
            after.job.join()
        }
    }
}

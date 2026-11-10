package com.snothin.ghostsam.ui.screen

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal data class HomeRootEvidence(
    val moduleLoaded: Boolean,
    val suAvailable: Boolean,
)

/** Recheck local su until it becomes available. This runs on each foreground entry,
 * independently of ADB/Shizuku, and never restores an su grant from a load receipt. */
internal suspend fun monitorHomeRoot(
    probeSu: suspend () -> Boolean,
    moduleLoaded: suspend () -> Boolean,
    onEvidence: suspend (HomeRootEvidence) -> Unit,
    awaitNextProbe: suspend () -> Unit = { delay(5_000L) },
) {
    var observedModule = false
    while (currentCoroutineContext().isActive) {
        val suAvailable = probeSu()
        observedModule = observedModule || suAvailable || moduleLoaded()
        onEvidence(HomeRootEvidence(observedModule, suAvailable))
        if (suAvailable) return
        awaitNextProbe()
    }
}

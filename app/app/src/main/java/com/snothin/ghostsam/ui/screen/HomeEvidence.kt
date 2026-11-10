package com.snothin.ghostsam.ui.screen

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.snothin.ghostsam.R
import com.snothin.ghostsam.data.exploit.BootStateFile
import com.snothin.ghostsam.data.exploit.PayloadScheme
import com.snothin.ghostsam.data.prefs.ExploitPrefs
import com.snothin.ghostsam.data.device.KernelSuProbe
import com.snothin.ghostsam.data.prefs.SettingsPrefs
import com.snothin.ghostsam.data.channel.SuController
import com.snothin.ghostsam.data.channel.SuDaemonChannel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class HomeEvidence internal constructor(
    private val context: Context,
) {
    private val initialModuleLoaded = KernelSuProbe.isActive() ||
        ExploitPrefs.isModuleLoadedThisBoot(context) ||
        (ExploitPrefs.isReceiptVerified(context, ExploitPrefs.currentBootToken()) &&
            ExploitPrefs.receiptMode(context) != "noksu")

    // KernelSU active: file probe + boot receipt (a matching boot_id counts as active across
    // app restarts; expires on reboot).
    var ksuActive by mutableStateOf(
        initialModuleLoaded || ExploitPrefs.isReceiptVerified(context, ExploitPrefs.currentBootToken()),
    )
        private set

    var moduleActive by mutableStateOf(initialModuleLoaded)
        private set

    // no-ksu session: receipt mode=noksu without manual upgrade, or su_daemon evidence; the
    // !isKsudUpgraded guard stops a fallback to temporary mode after an upgrade.
    var noksuSession by mutableStateOf(
        ExploitPrefs.receiptMode(context) == "noksu" && !ExploitPrefs.isKsudUpgraded(context),
    )
        private set

    var suAvailable by mutableStateOf(false)
        private set

    var suProbeDone by mutableStateOf(false)
        private set

    var suDenied by mutableStateOf(false)
        private set

    var managerInstalled by mutableStateOf(KernelSuProbe.isManagerInstalled(context))
        private set

    var ksudUpgraded by mutableStateOf(ExploitPrefs.isKsudUpgraded(context))
        private set

    var rootInfo by mutableStateOf("")
        private set

    suspend fun monitorLocalSu() = monitorHomeRoot(
        probeSu = { withContext(Dispatchers.IO) { SuController.isAvailable(forceRefresh = true) } },
        moduleLoaded = {
            withContext(Dispatchers.IO) {
                moduleActive || ExploitPrefs.isModuleLoadedThisBoot(context) || KernelSuProbe.isActive()
            }
        },
        onEvidence = { evidence ->
            if (!suProbeDone || suAvailable != evidence.suAvailable || moduleActive != evidence.moduleLoaded) {
                Log.i("GhostSam", "home root: module=${evidence.moduleLoaded}, app_su=${evidence.suAvailable}")
            }
            suAvailable = evidence.suAvailable
            suProbeDone = true
            if (evidence.moduleLoaded) {
                ksuActive = true
                moduleActive = true
                noksuSession = false
            }
            suDenied = evidence.moduleLoaded && !evidence.suAvailable
            if (ksuActive) confirmActive()
        },
    )

    suspend fun refreshManager() {
        managerInstalled = withContext(Dispatchers.IO) {
            KernelSuProbe.probeManagerInstalled(context)
        }
    }

    suspend fun pollUntilActive(channelConnected: Boolean) {
        // File probes are unreliable on Samsung builds: once the channel is up, verify the module
        // via shell uid + su uid=0.
        var backoffMs = 1_000L
        while (!ksuActive) {
            if (channelConnected) {
                val moduleOk = withContext(Dispatchers.IO) { KernelSuProbe.isActiveViaShizuku() }
                if (moduleOk) {
                    ksuActive = true
                    moduleActive = true
                    noksuSession = false
                } else if (!noksuSession) {
                    when (withContext(Dispatchers.IO) { KernelSuProbe.suAuthState() }) {
                        KernelSuProbe.SuAuthState.Rooted -> {
                            ksuActive = true
                            moduleActive = true
                            suDenied = false
                        }
                        KernelSuProbe.SuAuthState.Denied -> suDenied = true
                        KernelSuProbe.SuAuthState.Unavailable -> suDenied = false
                    }
                }
            }
            if (!ksuActive) {
                val chanOk = withContext(Dispatchers.IO) { SuDaemonChannel.isAvailable() }
                if (chanOk) {
                    ksuActive = true
                    moduleActive = false
                    noksuSession = true
                } else if (ExploitPrefs.isReceiptVerified(context, ExploitPrefs.currentBootToken())) {
                    ksuActive = true
                }
            }
            if (!ksuActive) {
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(5_000L)
            }
        }
        confirmActive()
    }

    suspend fun refreshRootInfo() {
        rootInfo = if (ksuActive) {
            val time = ExploitPrefs.successTime(context)
            val timeText = if (time > 0) {
                context.getString(R.string.home_root_success_at, formatRootTime(time))
            } else {
                context.getString(R.string.home_root_success_unknown)
            }
            val suffix = if (noksuSession && !moduleActive && !ksudUpgraded) {
                context.getString(R.string.home_root_tmp_mode)
            } else {
                if (suAvailable) context.getString(R.string.home_root_su_granted)
                else context.getString(R.string.home_root_su_denied)
            }
            "$timeText · $suffix"
        } else {
            ""
        }
    }

    // Receipt write-back after activation (idempotent): later, even without a channel, the
    // receipt alone identifies the existing root.
    suspend fun confirmActive() {
        val token = ExploitPrefs.currentBootToken()
        if (token != null && moduleActive) ExploitPrefs.storeModuleLoaded(context, token)
        if (!suAvailable && !noksuSession) return
        if (token != null && (!ExploitPrefs.isReceiptVerified(context, token) ||
                (moduleActive && ExploitPrefs.receiptMode(context) == "noksu"))) {
            // Receipt mode follows the live form; a missing/stale file falls back by route (DFR's end
            // state is KSU; a stale noksu once mislabeled an upgraded session).
            val state = withContext(Dispatchers.IO) { BootStateFile.read(context) }
            val mode = when {
                moduleActive || ksudUpgraded -> "ksu"
                state?.bootId == token -> state.mode ?: "ksu"
                SettingsPrefs.payloadScheme(context) != PayloadScheme.Default -> "ksu"
                else -> if (SettingsPrefs.skipKsu(context)) "noksu" else "ksu"
            }
            ExploitPrefs.storeReceipt(context, token, mode)
            noksuSession = mode == "noksu"
        }
        if (ExploitPrefs.successTime(context) == 0L) syncSuccessTimeFromBoot()
    }

    // Upgrade wrap-up: boot-level mark -> receipt mode sync to ksu (a restart would otherwise
    // restore the temporary session and SELinux would lock the toggle).
    fun finalizeUpgrade() {
        ExploitPrefs.storeKsudUpgraded(context)
        ExploitPrefs.currentBootToken()?.let { ExploitPrefs.storeReceipt(context, it, "ksu") }
        ksudUpgraded = true
        moduleActive = true
        noksuSession = false
        suDenied = false
    }

    private suspend fun syncSuccessTimeFromBoot() {
        val state = withContext(Dispatchers.IO) { BootStateFile.read(context) } ?: return
        val uptime = state.uptime ?: return
        if (state.bootId != ExploitPrefs.currentBootToken() || state.status != "0") return
        val nowUptime = runCatching {
            File("/proc/uptime").readText().trim().substringBefore('.').toLongOrNull()
        }.getOrNull() ?: return
        val elapsedMs = (nowUptime - uptime).coerceAtLeast(0L) * 1000
        ExploitPrefs.setSuccessTime(context, System.currentTimeMillis() - elapsedMs)
    }
}

private const val MANAGER_POLL_MILLIS = 10_000L

private val rootTimeFormat by lazy { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

private fun formatRootTime(millis: Long): String = rootTimeFormat.format(Date(millis))

@Composable
internal fun rememberHomeEvidence(channelConnected: Boolean): HomeEvidence {
    val appContext = LocalContext.current.applicationContext
    val evidence = remember { HomeEvidence(appContext) }
    // Lifecycle gating: Compose effects do not pause on STOP; keep the loop STARTED-gated
    // (it used to keep running in the background).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        // repeatOnLifecycle also starts when this effect is attached after ON_RESUME.
        // A callback-only observer can miss that first foreground entry.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            evidence.refreshManager()
            evidence.monitorLocalSu()
        }
    }
    LaunchedEffect(channelConnected, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { evidence.pollUntilActive(channelConnected) }
            launch {
                while (true) {
                    delay(MANAGER_POLL_MILLIS)
                    evidence.refreshManager()
                }
            }
        }
    }
    LaunchedEffect(
        evidence.ksuActive,
        evidence.noksuSession,
        evidence.suAvailable,
        evidence.ksudUpgraded,
    ) {
        evidence.refreshRootInfo()
    }
    return evidence
}

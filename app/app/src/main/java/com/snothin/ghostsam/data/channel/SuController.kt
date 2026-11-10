package com.snothin.ghostsam.data.channel

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** su (root) channel, parallel to Shizuku: KernelSU's su is independent of Shizuku;
 *  unified entry is [ShizukuController.execPrivileged]. */
object SuController {

    // KernelSU's sucompat intercepts this path even when no su file is visible.
    private const val SU_PATH = "/system/bin/su"

    private const val CACHE_TTL_OK_MILLIS = Long.MAX_VALUE

    private const val CACHE_TTL_FAIL_MILLIS = 10_000L

    private const val PROBE_TIMEOUT_MILLIS = 10_000L

    private val probeLock = Any()

    @Volatile
    private var cachedAvailable: Boolean? = null

    @Volatile
    private var cachedAtMillis = 0L

    fun invalidateCache() {
        cachedAvailable = null
    }

    /** Is su available: `/system/bin/su -c id` answers uid=0; cached process-wide (success
     * forever, failure 10s). Foreground checks bypass both cached outcomes.
     *  IO THREADS ONLY (forks su and holds the single-flight lock; main thread = ANR). */
    fun isAvailable(forceRefresh: Boolean = false): Boolean {
        val cached = cachedAvailable
        if (!forceRefresh && cached != null) {
            val ttl = if (cached) CACHE_TTL_OK_MILLIS else CACHE_TTL_FAIL_MILLIS
            if (SystemClock.elapsedRealtime() - cachedAtMillis < ttl) return cached
        }
        synchronized(probeLock) {
            val doubleCheck = cachedAvailable
            if (!forceRefresh && doubleCheck != null) {
                val ttl = if (doubleCheck) CACHE_TTL_OK_MILLIS else CACHE_TTL_FAIL_MILLIS
                if (SystemClock.elapsedRealtime() - cachedAtMillis < ttl) return doubleCheck
            }
            val result = runCatching {
                val process = Runtime.getRuntime().exec(arrayOf(SU_PATH, "-c", "id"))
                try {
                    val output = collectProcessOutput(process, timeoutMillis = PROBE_TIMEOUT_MILLIS)
                    output.lineSequence().any { it == "uid=0" || it.startsWith("uid=0(") || it.startsWith("uid=0 ") }
                } finally {
                    if (process.isAlive) process.destroy()
                }
            }.getOrDefault(false)
            cachedAvailable = result
            cachedAtMillis = SystemClock.elapsedRealtime()
            return result
        }
    }

    suspend fun execCommand(
        command: String,
        timeoutMillis: Long = 20_000,
    ): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf(SU_PATH, "-c", command))
            try {
                collectProcessOutput(process, timeoutMillis)
            } finally {
                if (process.isAlive) process.destroy()
            }
        } catch (error: Throwable) {
            channelError(error)
        }
    }

    fun spawnShell(): Process? = runCatching {
        Runtime.getRuntime().exec(arrayOf(SU_PATH))
    }.getOrNull()
}

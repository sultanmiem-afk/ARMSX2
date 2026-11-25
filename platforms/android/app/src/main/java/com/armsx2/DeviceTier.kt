package com.armsx2

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Best-effort device-capability probe used to seed low-end-friendly defaults
 * and presets. Every reader is defensive: any probe that throws or returns a
 * junk value falls back to a conservative assumption (NOT low-end) so we never
 * hobble a capable device on a bad read.
 *
 * The tier is only ever a *hint* — it seeds first-run wizard defaults and the
 * one-tap "Low-End" preset. Users can always override in Settings, so a false
 * negative just means "no auto-help offered", never a hard cap.
 */
object DeviceTier {
    /** CPU core count reported by the runtime. Clamped to a sane 1..64 range;
     *  a bad read falls back to 8 (assume a capable device). */
    fun coreCount(): Int = try {
        Runtime.getRuntime().availableProcessors().coerceIn(1, 64)
    } catch (_: Throwable) {
        8
    }

    /** MTVU (multi-threaded VU1) is a win only when there are spare cores to run
     *  VU1 on its own thread. On 4-core / big.LITTLE budget SoCs it can cost more
     *  than it saves (EE<->VU1 sync + thread hop), so we gate the *default* on a
     *  6-core minimum. This does NOT change the persisted Settings default (which
     *  would bleed into existing users' saved configs) — it's applied only in
     *  first-run wizard defaults and the Low-End preset. */
    fun mtvuDefault(): Boolean = coreCount() >= 6

    /**
     * Whether the IOP thread setting should start on for this device: its performance tier is two or three cores and
     * the emulator core would run the thread (IopThread::DefaultOnForHost, asked through JNI so the rule is written once).
     * [mtvu] is the MTVU setting the caller has, which counts one more thread in the core's rule. False when the native
     * library did not load or the call fails: the setting stays off, as it was.
     *
     * This does NOT change the persisted Settings default (a save written by an older build holds the old value): it
     * seeds a new install ([com.armsx2.config.ConfigStore.seedFreshInstallDefaults]), moves an existing one once
     * ([com.armsx2.config.ConfigStore.migrateIopThreadNarrowTier]) and answers a Reset.
     */
    fun iopThreadDefault(mtvu: Boolean = true): Boolean = try {
        !kr.co.iefriends.pcsx2.NativeApp.hasNoNativeBinary && kr.co.iefriends.pcsx2.NativeApp.iopThreadDefaultOn(mtvu)
    } catch (_: Throwable) {
        false
    }

    /** Total physical RAM in bytes, or Long.MAX_VALUE if it can't be read (so a
     *  bad read never trips the low-RAM branch). */
    private fun totalMemBytes(context: Context): Long = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null) {
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            mi.totalMem
        } else Long.MAX_VALUE
    } catch (_: Throwable) {
        Long.MAX_VALUE
    }

    private fun isLowRam(context: Context): Boolean = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.isLowRamDevice ?: false
    } catch (_: Throwable) {
        false
    }

    /**
     * Heuristic: a device is "low-end" for PS2 emulation when ANY of:
     *   - the OS flags it as a low-RAM device (isLowRamDevice), OR
     *   - it has fewer than 6 CPU cores (no headroom for MTVU + GS thread), OR
     *   - it has under ~4 GB total RAM.
     *
     * Deliberately OR-ed and lenient: on any probe failure the individual
     * checks default to "not low-end", so we only flag a device we're fairly
     * sure is weak. Callers use this to *recommend* (never force) the Low-End
     * preset / Fast profile in the setup wizard.
     */
    fun isLowEnd(context: Context): Boolean = try {
        val lowRam = isLowRam(context)
        val fewCores = coreCount() < 6
        // ~4 GB with a little slack for reserved/kernel memory (report ~3.7 GB
        // on a nominal 4 GB device), so genuine 4 GB devices aren't flagged.
        val lowMem = totalMemBytes(context) < 3_600_000_000L
        lowRam || fewCores || lowMem
    } catch (_: Throwable) {
        false
    }

    /** True only when Android identifies the MediaTek Helio G99 (MT6789).
     *  Used to expose its opt-in emulator preset; it never forces settings. */
    fun isHelioG99(): Boolean {
        return isHelioG99Model(socModel())
    }

    /** Pure matcher kept separate so SoC identification can be covered by JVM tests. */
    internal fun isHelioG99Model(model: String): Boolean {
        val normalized = model.lowercase()
        return normalized.contains("mt6789") || normalized.contains("helio g99")
    }

    /** Human-readable SoC/hardware id for logging/diagnostics. Uses
     *  Build.SOC_MODEL on API 31+, else Build.HARDWARE. */
    fun socModel(): String = try {
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null
        firstKnownSocModel(soc, Build.HARDWARE)
    } catch (_: Throwable) {
        "unknown"
    }

    /** Prefer the SoC model, but fall back when Android reports it as blank or "unknown". */
    internal fun firstKnownSocModel(socModel: String?, hardware: String?): String {
        return listOf(socModel, hardware)
            .firstOrNull { !it.isNullOrBlank() && !it.equals("unknown", ignoreCase = true) }
            ?: "unknown"
    }
}

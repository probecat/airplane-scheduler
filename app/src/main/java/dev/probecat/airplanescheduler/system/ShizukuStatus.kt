package dev.probecat.airplanescheduler.system

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

enum class ShizukuState { READY, OFFLINE, PERMISSION_NEEDED }

object ShizukuStatus {
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST = 1

    fun current(): ShizukuState = try {
        when {
            !Shizuku.pingBinder() -> ShizukuState.OFFLINE
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuState.READY
            else -> ShizukuState.PERMISSION_NEEDED
        }
    } catch (_: RuntimeException) {
        ShizukuState.OFFLINE
    }

    fun isReady(): Boolean = current() == ShizukuState.READY

    // Shows Shizuku's permission dialog. Returns false when it can't, such as after "Deny and don't ask again".
    fun request(): Boolean = try {
        when {
            !Shizuku.pingBinder() -> false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> true
            Shizuku.shouldShowRequestPermissionRationale() -> false
            else -> {
                Shizuku.requestPermission(REQUEST)
                true
            }
        }
    } catch (_: RuntimeException) {
        false
    }
}

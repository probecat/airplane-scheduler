package dev.probecat.airplanescheduler.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

fun notificationsAllowed(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
